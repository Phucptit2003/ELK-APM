pipeline {
  agent any

  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    timeout(time: 40, unit: 'MINUTES')
    disableConcurrentBuilds()
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        echo "Workspace: ${env.WORKSPACE}"
      }
    }

    stage('Host Tune (one-time safe)') {
      steps {
        // Không fail nếu đã cấu hình trước đó
        sh '''
          set -e
          if [ -w /etc/sysctl.d ]; then
            echo 'vm.max_map_count=262144' | sudo tee /etc/sysctl.d/99-elasticsearch.conf >/dev/null || true
            sudo sysctl --system || true
          fi
        '''
      }
    }

    stage('Docker Build (App)') {
      steps {
        sh '''
          docker version
          docker-compose version || true
          # Build image app (multi-stage Dockerfile)
          docker build -t spring-elk-demo:latest -t spring-elk-demo:${BUILD_NUMBER} .
        '''
      }
    }

    stage('Start/Update Stack') {
      steps {
        sh '''
          # Lần đầu: khởi động toàn bộ stack (nếu chưa chạy)
          docker-compose up -d || true
          # Các lần sau: chỉ cập nhật app, không đụng ELK/MySQL/APM
          docker-compose up -d --no-deps spring-app
        '''
      }
    }

    stage('Health Check (App)') {
      steps {
        sh '''
          echo "Waiting for app to be healthy..."
          for i in {1..36}; do
            sleep 5
            if curl -sf http://localhost:8080/actuator/health | grep -q UP; then
              echo "App is healthy"; exit 0
            fi
          done
          echo "App not healthy after ~3 minutes" >&2; exit 1
        '''
      }
    }

    stage('Smoke Test') {
      steps {
        sh '''
          STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/users)
          [ "$STATUS" = "200" ] || { echo "GET /api/users => $STATUS"; exit 1; }
          echo "GET /api/users => 200"
        '''
      }
    }
  }

  post {
    success {
      echo "✅ Deploy thành công build #${BUILD_NUMBER}"
      echo "- App:     http://54.206.9.89:8080"
      echo "- Kibana:  http://54.206.9.89:5601 (user: elastic, pass: changeme — đổi sau demo!)"
    }
    failure {
      echo "❌ Deploy thất bại, kiểm tra logs phía trên."
    }
  }
}
