node {
   // Mark the code checkout 'stage'....
   stage 'Checkout'

   // Get some code from a GitHub repository
   git url: 'https://github.com/mploed/cd-demo-credit-application.git'

   // Get the maven tool.
   // ** NOTE: This 'M3' maven tool must be configured
   // **       in the global configuration.
   def mvnHome = tool 'M3'

   // Mark the code build 'stage'....
   stage 'Build'
   // Run the maven build
   sh "${mvnHome}/bin/mvn -Dmaven.test.failure.ignore clean package"



   stage 'Deploy and Sonar'

   parallel (
      "Push to CF": {
         withCredentials([usernamePassword(credentialsId: '3c8d45c3-9168-46c0-ac8d-fad8eefa8f8c', passwordVariable: 'CF_PASS', usernameVariable: 'CF_USER')]) {
            sh "/home/jenkins/cf login -a https://api.run.pivotal.io -u ${CF_USER} -p ${CF_PASS} -s adp"
            sh "/home/jenkins/cf push"
         }
      },
      "SonarQube analysis": {
         def scannerHome = tool 'SonarQube Scanner 3.0';
         withSonarQubeEnv('Local SonarQube') {
            sh "${scannerHome}/bin/sonar-scanner"
         }
      }
   )

}
