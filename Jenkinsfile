pipeline {

  options {
    // Discard everything except the last 10 builds
    buildDiscarder(logRotator(numToKeepStr: '10'))
    // Don't build the same branch concurrently
    disableConcurrentBuilds()
  }

  agent any

  stages {

    stage('Git checkout') {
      steps {

        // Get some code from a GitHub repository
        dir('COVID') {
          git 'https://github.com/CSSEGISandData/COVID-19'
        }

        git 'https://github.com/lh32469/Corona'
        sh "ls -l"

      }
    }


    stage('Build') {
      agent {
        docker {
          reuseNode true
          image 'maven:latest'
          args '-u root -v /var/lib/jenkins/.m2:/root/.m2'
        }
      }
      steps {
        sh 'mvn clean package'
      }
    }

    stage('Build Docker Image') {
      environment {
        registry = "corona/master"
        registryCredential = 'dockerhub'
      }
      steps {
        // unstash 'jars'
        sh 'ls -l target'
        script {
          image = docker.build registry + ":$BUILD_NUMBER"
        }
        // Cleanup previous images older than 12 hours
        sh 'docker image prune -af --filter "label=app.name=corona" --filter "until=12h"'
      }
    }

    stage('Stop Docker Image') {
      steps {
        sh 'docker stop corona-master || true && docker rm corona-master || true'
      }
    }

    stage('Run Docker Image') {
      steps {
        sh 'docker run -d -p 2019:8080 ' +
            '--restart=always ' +
            '--dns=172.17.0.1 ' +
            '--name corona-master ' +
            'corona/master:$BUILD_NUMBER'
      }
    }

    stage('Register Consul Service') {
      steps {
        script {
          consul = "http://127.0.0.1:8500/v1/agent/service/register"
          ip = sh(
              returnStdout: true,
              script: "docker inspect corona-master | jq '.[].NetworkSettings.Networks.bridge.IPAddress'"
          )
          def service = readJSON text: '{ "Port": 3000 }'
          service["Address"] = ip.toString().trim() replaceAll("\"", "");
          service["Name"] = "corona-master".toString()
          writeJSON file: 'service.json', json: service, pretty: 3
          sh(script: "cat service.json")
          sh(script: "curl -X PUT -d @service.json " + consul)
        }
      }
    }

  }

}
