## Running the rimrock application

* Checkout the project
* Right-click `pl.cyfronet.rimrock.RimrockApplication` and run as Java application
* That is it :)

To enable code hot-deployment add the following as VM arguments in the Run configurations... dialog (in eclipse):

`-javaagent:{path_to_springloaded_jar}/springloaded-1.2.0.RELEASE.jar -noverify`

Fix the path above accordingly (you can get the jar with the help of maven: `mvn dependency:get -Dartifact="org.springframework:springloaded:1.2.0.RELEASE"`).

## Testing the basic run RESt method

After the application is started you can fetch the user proxy (e.g. from DataNet), save it to a file and use the following commands to execute something on the UI machine:

	proxy="`cat /home/daniel/temp/user-proxy.pem | awk 1 ORS='\\\n'`"
	message="{\"host\":\"zeus.cyfronet.pl\", \"command\":\"pwd\", \"proxy\":\"$proxy\"}"
	echo $message > message.txt
	curl -X POST --data-binary @message.txt --header "Content-Type:application/json" http://localhost:8080/run