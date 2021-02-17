// project should be the last token of the Git repo URL in lowercase.
def project = "corona"
def port = "8085"
def branch = BRANCH_NAME.toLowerCase()
def svcId =  project + "-" + branch + "-" + BUILD_NUMBER

// Previously running container(s)
def running = ""


pipeline {

  options {
    // Discard everything except the last 10 builds
    buildDiscarder(logRotator(numToKeepStr: '10'))
    // Don't build the same branch concurrently
    disableConcurrentBuilds()

    // Cleanup orphaned branch Docker container
    branchTearDownExecutor 'CleanupDocker'
  }

  agent any

  stages {

    stage('Compile') {
      agent {
        docker {
          reuseNode true
          image 'maven:latest'
          args '-u root -v /var/lib/jenkins/.m2:/root/.m2'
        }
      }
      steps {
        sh 'mvn -B -DskipTests clean compile'
      }
    }

    stage('Test/Package') {
      agent {
        docker {
          reuseNode true
          image 'maven:latest'
          args '--dns=172.17.0.1 -u root -v /var/lib/jenkins/.m2:/root/.m2'
        }
      }
      steps {
        sh 'mvn -B package'
        junit '**/target/surefire-reports/TEST-*.xml'
      }
    }

    stage('Build New Docker') {
      environment {
        registryCredential = 'dockerhub'
      }
      steps {
        sh 'ls -l target'
        script {
          // Get all matching containers currently running
          running = sh(
              returnStdout: true,
              script: "docker ps -q --filter label=branch=$branch --filter label=app.name=$project"
          )
          image = docker.build("$project/$branch:$BUILD_NUMBER \
              --label app.name=$project \
              --label branch=$branch")
        }
        // Cleanup previous images older than 12 hours
        sh "docker image prune -af \
              --filter label=app.name=$project \
              --filter label=branch=$branch \
              --filter until=12h"
      }
    }

    stage('Start New Docker') {
      steps {
        sh 'docker run -d ' +
            '--restart=always ' +
            '--dns=172.17.0.1 ' +
            "--name $svcId " +
            '-e TZ=America/Los_Angeles ' +
            "-e SERVER_SERVLET_CONTEXT_PATH=/$branch " +
            "-e BRANCH=$branch " +
            "$project/$branch:$BUILD_NUMBER"
      }
    }

    stage('Test/Register New Docker') {
      steps {
        sh "sleep 30"
        script {
          // Get Docker instance IP address.
          ip = sh(
              returnStdout: true,
              script: "docker inspect $project-$branch-$BUILD_NUMBER | jq '.[].NetworkSettings.Networks.bridge.IPAddress'"
          )
          // Test new Docker instance directly
          url = ip.trim() + ":$port"
          sh "curl -f ${url}/$branch/actuator/health > /dev/null"
        }
      }
    }

    stage('Stop Previous Docker') {
      steps {
        script {
          running = running.trim()
          running = running.replace("\n", " ").replace("\r", " ");
          if(!running.isEmpty()) {
            sh "docker stop $running"
            sh "docker rm $running"
          }
        }
      }
    }

  }

  post {
    always {
      // Cleanup Jenkins workspace
      cleanWs()
    }
  }

}
