import requests
import threading
import subprocess
import time
import json
import sys

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
	def __init__(self, url, inputStream, outputThread, errorThread):
		super(RestThread, self).__init__()
		self.url = url
		self.inputStream = inputStream
		self.outputThread = outputThread
		self.errorThread = errorThread

	def run(self):
		while self.outputThread.available() or self.errorThread.available():
			output = self.outputThread.getAvailableBytes()
			error = self.errorThread.getAvailableBytes()
			payload = {'standard_output': output, 'standard_error': error}
			headers = {'content-type': 'application/json'}
			response = requests.post(self.url, data = json.dumps(payload), headers = headers)
			cmd = response.json()['input']
			if cmd:
				self.inputStream.write(cmd + '\n')
			time.sleep(0.5)

url = sys.argv[1]
command = sys.argv[2]

process = subprocess.Popen([command], stdin = subprocess.PIPE, stdout = subprocess.PIPE,
							stderr = subprocess.PIPE, universal_newlines = True)
outThread = OutputThread(process.stdout)
errThread = OutputThread(process.stderr)
restThread = RestThread(url, process.stdin, outThread, errThread)

outThread.start()
errThread.start()
restThread.start()

outThread.join()
errThread.join()
restThread.join()