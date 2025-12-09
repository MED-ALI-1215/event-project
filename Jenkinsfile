pipeline {
    agent any
    
    tools {
        maven 'Maven'
        jdk 'JDK17'
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git --version'
                sh 'mvn --version'
                sh 'java --version'
            }
        }
        
        stage('Build') {
            steps {
                sh './mvnw clean compile -DskipTests'
            }
        }
        
        stage('Test') {
            steps {
                sh './mvnw test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/surefire-reports/*.xml', fingerprint: true
                }
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh './mvnw sonar:sonar \
                        -Dsonar.projectKey=events-project \
                        -Dsonar.projectName="Events Project" \
                        -Dsonar.java.binaries=target/classes'
                }
            }
        }
        
        
        stage('Package') {
            steps {
                sh './mvnw clean package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
        
        stage('Docker Build') {
            steps {
                script {
                    sh 'docker --version'
                    sh 'docker build -t events-project:${BUILD_NUMBER} .'
                    sh 'docker tag events-project:${BUILD_NUMBER} events-project:latest'
                }
            }
        }
        
        stage('Docker Test') {
            steps {
                script {
                    sh 'docker stop events-test-${BUILD_NUMBER} || true'
                    sh 'docker rm events-test-${BUILD_NUMBER} || true'
                    
                    sh 'docker run -d --name events-test-${BUILD_NUMBER} -p 809${BUILD_NUMBER}:8080 events-project:${BUILD_NUMBER}'
                    
                    sleep 30
                    sh '''
                        if curl -s http://localhost:809${BUILD_NUMBER}/events/actuator/health > /dev/null; then
                            echo "✅ Docker container test PASSED"
                        else
                            echo "❌ Docker container test FAILED"
                            docker logs events-test-${BUILD_NUMBER}
                            exit 1
                        fi
                    '''
                }
            }
            post {
                always {
                    sh 'docker stop events-test-${BUILD_NUMBER} || true'
                    sh 'docker rm events-test-${BUILD_NUMBER} || true'
                }
            }
        }
        
        stage('Cleanup') {
            steps {
                sh 'docker system prune -f'
                sh 'rm -rf target/* || true'
            }
        }
    }
    
    post {
        success {
            echo '🎉 Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed!'
        }
        always {
            echo 'Pipeline completed'
            cleanWs()
        }
    }
}
