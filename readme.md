## Running the rimrock application in development mode

* Checkout the project
* Right-click `pl.cyfronet.rimrock.RimrockApplication` and run as Java application
* That is it :)

To enable code hot-deployment add the following as VM arguments in the Run configurations... dialog (in eclipse):

`-javaagent:{path_to_springloaded_jar}/springloaded-1.2.0.RELEASE.jar -noverify`

Fix the path above accordingly (you can get the jar with the help of maven: `mvn dependency:get -Dartifact="org.springframework:springloaded:1.2.0.RELEASE"`).

## Testing the basic run REST method

After the application is started you can fetch the user proxy (e.g. from DataNet), save it to a file and use the following commands to execute something on the UI machine:

	proxy="`cat {path-to-proxy-file} | base64 | tr -d '\n'`"
	message="{\"host\":\"zeus.cyfronet.pl\", \"command\":\"pwd\"}"
	echo $message > message.txt
	curl -X GET --data-binary @message.txt --header "Content-Type:application/json" --header "PROXY:$proxy" http://localhost:8080/api/process

## Configuring proxy generation for integration tests

* Create `src/test/resources/config/application.properties` file and put the following inside:

    test.proxy.path = [path_to_already_generated_proxy]
    
**OR**    

* Create `src/test/resources/config/application.properties` file and put the following inside:

    test.user.key.pass = [user_key_pass]

* Put `usercert.pem` and `userkey.pem` files in the `src/test/resources` directory

Note that all these resources are ignored so no private data leaks through git.

##Running the rimrock application in production

* Build the final jar with `mvn clean package`.
* Copy the jar file to a production server and run `java -jar {jar-file-path}` (no tomcat required).