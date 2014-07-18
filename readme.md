## Running the rimrock application

* Checkout the project
* Right-click `pl.cyfronet.rimrock.RimrockApplication` and run as Java application
* That is it :)

To enable code hot-deployment add the following as VM arguments in the Run configurations... dialog (in eclipse):

`java -javaagent:{path_to_springloaded_jar}/springloaded-1.2.0.RELEASE.jar -noverify`

Fix the path above accordingly.