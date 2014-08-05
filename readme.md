## Running the rimrock application

* Checkout the project
* Right-click `pl.cyfronet.rimrock.RimrockApplication` and run as Java application
* That is it :)

To enable code hot-deployment add the following as VM arguments in the Run configurations... dialog (in eclipse):

`-javaagent:{path_to_springloaded_jar}/springloaded-1.2.0.RELEASE.jar -noverify`

Fix the path above accordingly (you can get the jar with the help of maven: `mvn dependency:get -Dartifact="org.springframework:springloaded:1.2.0.RELEASE"`).

## Testing the basic run REST method

After the application is started you can fetch the user proxy (e.g. from DataNet), save it to a file and use the following commands to execute something on the UI machine:

	proxy="`cat /home/daniel/temp/user-proxy.pem | awk 1 ORS='\\\n'`"
	message="{\"host\":\"zeus.cyfronet.pl\", \"command\":\"pwd\", \"proxy\":\"$proxy\"}"
	echo $message > message.txt
	curl -X GET --data-binary @message.txt --header "Content-Type:application/json" http://localhost:8080/api/process

## Configuring proxy generation for integration tests

* Create `src/main/resources/config/application.properties` file and put the following inside:

    test.proxy.path = [path_to_already_generated_proxy]
    
**OR**    

* Create `src/main/resources/config/application.properties` file and put the following inside:

    test.user.key.pass = [user_key_pass]

* Put `usercert.pem` and `userkey.pem` files in the `src/main/resources` directory

Note that all these resources are ignored so no private data leaks through git.

## Configuring upload dir for file manager integration tests

* Create `src/main/resources/config/application.properties` file and put the following inside:
	
	test.uploadDir.path = [upload_dir]