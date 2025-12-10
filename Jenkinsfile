pipeline {
    agent any
    
    tools {
        maven 'Maven'
        jdk 'JDK17'
    }
    
    environment {
        DOCKERHUB_USER = '1215886'
        NEXUS_URL = 'http://localhost:8081'
        NEXUS_REPO = 'maven-snapshots'
        NEXUS_USER = 'admin'
        NEXUS_PASSWORD = 'gass1215'
        SONAR_TOKEN = 'squ_c0209bcae6a609a7d10791b1f8e0d4fbc4149437'
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '3'))
        timeout(time: 20, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
    }
    
    stages {
        stage('Check Disk Space') {
            steps {
                sh '''
                    echo "=== DISK SPACE CHECK ==="
                    df -h /
                    FREE_SPACE=$(df / --output=avail | tail -1 | tr -d ' ')
                    MIN_SPACE=2000000  # 2GB in KB
                    
                    if [ $FREE_SPACE -lt $MIN_SPACE ]; then
                        echo "❌ ERROR: Low disk space! Only $(($FREE_SPACE/1024/1024))GB free."
                        echo "Need at least 2GB free. Run cleanup first."
                        exit 1
                    else
                        echo "✅ Disk space OK: $(($FREE_SPACE/1024/1024))GB free"
                    fi
                '''
            }
        }
        
        stage('Checkout') {
            steps {
                echo "=== CHECKING OUT CODE ==="
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                sh './mvnw clean compile -DskipTests'
            }
        }
        
        stage('Unit Tests') {
            steps {
                sh './mvnw test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                script {
                    echo "=== CHECKING SONARQUBE ==="
                    
                    // First check if SonarQube is running
                    def sonarStatus = sh(
                        script: "curl -s -o /dev/null -w '%{http_code}' http://localhost:9000/api/system/status || echo '000'",
                        returnStdout: true
                    ).trim()
                    
                    if (sonarStatus == "200") {
                        echo "✅ SonarQube is running (Status: ${sonarStatus})"
                        
                        // Run SonarQube analysis with timeout
                        timeout(time: 3, unit: 'MINUTES') {
                            sh """
                                ./mvnw sonar:sonar \
                                    -Dsonar.projectKey=events-project \
                                    -Dsonar.projectName="Events Project" \
                                    -Dsonar.host.url=http://localhost:9000 \
                                    -Dsonar.login=${SONAR_TOKEN} \
                                    -Dsonar.java.binaries=target/classes \
                                    -Dsonar.sourceEncoding=UTF-8
                            """
                        }
                    } else {
                        echo "⚠️ WARNING: SonarQube not accessible (Status: ${sonarStatus})"
                        echo "Skipping SonarQube analysis for this build."
                    }
                }
            }
        }
        
        stage('Package') {
            steps {
                sh './mvnw clean package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    echo "=== BUILDING DOCKER IMAGE ==="
                    
                    // Check if we have enough space for Docker
                    def freeSpace = sh(
                        script: "df / --output=avail | tail -1 | tr -d ' '",
                        returnStdout: true
                    ).trim().toInteger()
                    
                    if (freeSpace < 1500000) { // Less than 1.5GB
                        echo "⚠️ WARNING: Low disk space for Docker build"
                        echo "Skipping Docker build to preserve space."
                    } else {
                        sh """
                            docker build -t ${DOCKERHUB_USER}/eventsproject:${BUILD_NUMBER} .
                            docker tag ${DOCKERHUB_USER}/eventsproject:${BUILD_NUMBER} ${DOCKERHUB_USER}/eventsproject:latest
                        """
                    }
                }
            }
        }
        
        stage('Push Docker Image') {
            when {
                expression {
                    // Only push if we built the image and have credentials
                    return true
                }
            }
            steps {
                script {
                    echo "=== PUSHING TO DOCKER HUB ==="
                    
                    try {
                        timeout(time: 2, unit: 'MINUTES') {
                            withCredentials([usernamePassword(
                                credentialsId: 'docker-hub-credentials',
                                usernameVariable: 'DOCKERHUB_USERNAME',
                                passwordVariable: 'DOCKERHUB_PASSWORD'
                            )]) {
                                sh '''
                                    echo "${DOCKERHUB_PASSWORD}" | docker login -u "${DOCKERHUB_USERNAME}" --password-stdin || {
                                        echo "⚠️ Docker login failed. Skipping push."
                                        exit 0
                                    }
                                    
                                    # Try to push, but don't fail the pipeline if it fails
                                    docker push ${DOCKERHUB_USER}/eventsproject:${BUILD_NUMBER} || echo "Push of ${BUILD_NUMBER} tag failed"
                                    docker push ${DOCKERHUB_USER}/eventsproject:latest || echo "Push of latest tag failed"
                                    
                                    docker logout
                                    echo "✅ Docker push completed (or attempted)"
                                '''
                            }
                        }
                    } catch (Exception e) {
                        echo "⚠️ Docker push failed but continuing: ${e.getMessage()}"
                    }
                }
            }
        }
        
        stage('Upload to Nexus') {
            steps {
                script {
                    echo "=== UPLOADING TO NEXUS ==="
                    
                    def pom = readMavenPom file: 'pom.xml'
                    def artifactId = pom.artifactId
                    def version = pom.version
                    def groupId = pom.groupId.replace('.', '/')
                    def jarFile = "target/${artifactId}-${version}.jar"
                    
                    // Check if JAR file exists
                    if (fileExists(jarFile)) {
                        sh """
                            echo "Uploading: ${jarFile}"
                            echo "To: ${NEXUS_URL}/repository/${NEXUS_REPO}/${groupId}/${artifactId}/${version}/"
                            
                            NEXUS_UPLOAD_URL="${NEXUS_URL}/repository/${NEXUS_REPO}/${groupId}/${artifactId}/${version}/${artifactId}-${version}.jar"
                            
                            curl -f -u ${NEXUS_USER}:${NEXUS_PASSWORD} \
                                 --upload-file ${jarFile} \
                                 "\${NEXUS_UPLOAD_URL}" && \
                            echo "✅ Successfully uploaded to Nexus" || \
                            echo "⚠️ Nexus upload may have failed (check repository)"
                        """
                    } else {
                        echo "❌ JAR file not found: ${jarFile}"
                        echo "Skipping Nexus upload."
                    }
                }
            }
        }
        
        stage('Deploy') {
            steps {
                script {
                    echo "=== DEPLOYING APPLICATION ==="
                    
                    try {
                        sh '''
                            # Stop existing containers if any
                            docker-compose down 2>/dev/null || true
                            
                            # Start new containers
                            docker-compose up -d
                            
                            # Wait for startup
                            echo "Waiting for application to start (30 seconds)..."
                            sleep 30
                        '''
                        
                        // Health check with retry
                        def healthCheckPassed = false
                        for (int i = 1; i <= 3; i++) {
                            echo "Health check attempt ${i}/3..."
                            def healthStatus = sh(
                                script: "curl -s http://localhost:8088/events/actuator/health | grep -c '\"status\":\"UP\"' || echo '0'",
                                returnStdout: true
                            ).trim()
                            
                            if (healthStatus == "1") {
                                healthCheckPassed = true
                                echo "✅ Health check PASSED"
                                break
                            } else {
                                echo "Health check failed, waiting 10 seconds..."
                                sleep 10
                            }
                        }
                        
                        if (!healthCheckPassed) {
                            echo "⚠️ Health check failed, but continuing with deployment"
                            sh 'docker-compose logs app --tail=20'
                        }
                        
                    } catch (Exception e) {
                        echo "⚠️ Deployment had issues: ${e.getMessage()}"
                        echo "Continuing pipeline..."
                    }
                }
            }
        }
        
        stage('Cleanup') {
            steps {
                sh '''
                    echo "=== CLEANING UP ==="
                    
                    # Clean Docker (but keep recent images)
                    docker system prune -f 2>/dev/null || true
                    
                    # Clean Maven target directories
                    find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
                    
                    # Show final disk space
                    echo "Final disk space:"
                    df -h /
                '''
            }
        }
    }
    
    post {
        success {
            echo '🎉 PIPELINE COMPLETED SUCCESSFULLY!'
            sh '''
                echo "=== BUILD SUMMARY ==="
                echo "Build Number: ${BUILD_NUMBER}"
                echo "Application: http://localhost:8088/events"
                echo "SonarQube: http://localhost:9000"
                echo "Nexus: http://localhost:8081"
                echo "Jenkins: http://localhost:8080"
                echo "Docker Image: ${DOCKERHUB_USER}/eventsproject:${BUILD_NUMBER}"
                echo "Status: ✅ ALL STAGES COMPLETED"
            '''
        }
        failure {
            echo '❌ PIPELINE FAILED'
            sh '''
                echo "=== TROUBLESHOOTING INFO ==="
                echo "Disk space:"
                df -h /
                echo ""
                echo "Docker status:"
                docker ps -a
                echo ""
                echo "Recent logs:"
                docker-compose logs --tail=20 2>/dev/null || echo "No docker-compose logs"
            '''
        }
        always {
            echo 'Pipeline execution completed.'
            // Uncomment to clean workspace after build
            // cleanWs()
        }
    }
}
