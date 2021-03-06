import requests
import threading
import subprocess
import time
import json
import sys
import os
import traceback

class OutputThread(threading.Thread):
	def __init__(self, outStream):
		super(OutputThread, self).__init__()
		self.outStream = outStream
		self.outputChunks = []
		
	def run(self):
		byte = self.outStream.read(1)
		while byte:
			self.outputChunks.append(byte)
			byte = self.outStream.read(1)
	
	def getAvailableBytes(self):
		temp = self.outputChunks
		self.outputChunks = []
		
		return ''.join(temp)
	
	def available(self):
		return len(self.outputChunks) > 0 or self.is_alive()

class RestThread(threading.Thread):
	def __init__(self, url, secret, processId, inputStream, outputThread, errorThread, timeoutSeconds, process, certFilePath):
		super(RestThread, self).__init__()
		self.url = url
		self.secret = secret
		self.processId = processId
		self.inputStream = inputStream
		self.outputThread = outputThread
		self.errorThread = errorThread
		self.timeoutSeconds = timeoutSeconds
		self.process = process
		self.certFilePath = certFilePath

	def run(self):
		try:
			self.time = time.time()
			while self.outputThread.available() or self.errorThread.available():
				output = self.outputThread.getAvailableBytes()
				error = self.errorThread.getAvailableBytes()
				payload = {'standard_output': output, 'standard_error': error, 'secret': self.secret, 'process_id': self.processId}
				headers = {'content-type': 'application/json'}
				response = requests.post(self.url, data = json.dumps(payload), headers = headers, verify = self.certFilePath)
				cmd = response.json()['input']
				if cmd:
					self.inputStream.write(cmd + '\n')
					self.inputStream.flush()
				if cmd or output or error:
					self.time = time.time()
				if time.time() - self.time > self.timeoutSeconds:
					self.process.kill()
					payload = {'standard_output': '', 'standard_error': 'Timeout occurred', 'secret': self.secret, 'process_id': self.processId, 'finished': True}
					headers = {'content-type': 'application/json'}
					requests.post(self.url, data = json.dumps(payload), headers = headers, verify = self.certFilePath)
					return
				time.sleep(0.5)
			output = self.outputThread.getAvailableBytes()
			error = self.errorThread.getAvailableBytes()
			payload = {'standard_output': output, 'standard_error': error, 'secret': self.secret, 'process_id': self.processId, 'finished': True}
			headers = {'content-type': 'application/json'}
			requests.post(self.url, data = json.dumps(payload), headers = headers, verify = self.certFilePath)
		except Exception:
			with open('/tmp/iwrapper-' + self.processId + '.txt', 'w') as file:
				file.write("Error: " + traceback.format_exc())
		finally:
			os.remove(self.certFilePath)
			os.remove(os.path.realpath(__file__))

url = os.environ['url']
secret = os.environ['secret']
processId = os.environ['processId']
command = os.environ['command']
timeout = os.environ['timeout']
certFilePath = os.environ['certPath']

process = subprocess.Popen([command], stdin = subprocess.PIPE, stdout = subprocess.PIPE,
							stderr = subprocess.PIPE, universal_newlines = True)
outThread = OutputThread(process.stdout)
errThread = OutputThread(process.stderr)
restThread = RestThread(url, secret, processId, process.stdin, outThread, errThread, int(timeout), process, certFilePath)

outThread.start()
errThread.start()
restThread.start()

outThread.join()
errThread.join()
restThread.join()
