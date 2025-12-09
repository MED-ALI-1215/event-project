pipeline {
    agent any
    
    tools {
        maven 'Maven'  // Doit correspondre au nom dans Jenkins → Global Tool Configuration
        jdk 'JDK17'    // Doit correspondre au nom dans Jenkins
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
                script {
                    // Vérifiez si SonarQube est configuré
                    echo "SonarQube analysis would run here"
                    // Décommentez quand SonarQube est configuré :
                    // withSonarQubeEnv('SonarQube') {
                    //     sh './mvnw sonar:sonar -Dsonar.projectKey=events-project'
                    // }
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
                    // Arrêtez l'ancien conteneur si existant
                    sh 'docker stop events-test-${BUILD_NUMBER} || true'
                    sh 'docker rm events-test-${BUILD_NUMBER} || true'
                    
                    // Lancez le conteneur de test
                    sh 'docker run -d --name events-test-${BUILD_NUMBER} -p 809${BUILD_NUMBER}:8080 events-project:${BUILD_NUMBER}'
                    
                    // Attendez et testez
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
        
        stage('Deploy to Nexus') {
            steps {
                script {
                    echo "Deploying artifact to Nexus..."
                    // Décommentez quand Nexus est configuré :
                    // sh './mvnw deploy -DskipTests'
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
            emailext (
                subject: "✅ Pipeline Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "The pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER} completed successfully.\n\nCheck: ${env.BUILD_URL}",
                to: 'user@example.com'
            )
        }
        failure {
            echo '❌ Pipeline failed!'
            emailext (
                subject: "❌ Pipeline Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "The pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER} failed.\n\nCheck: ${env.BUILD_URL}",
                to: 'user@example.com'
            )
        }
        unstable {
            echo 'Pipeline marked as unstable'
        }
        always {
            echo 'Pipeline completed'
            cleanWs()
        }
    }
}
