node {

    tools {
        maven 'Maven 3.3.9'
        jdk 'jdk8'
    }

    if (!(env.BRANCH_NAME == 'master' || env.BRANCH_NAME.startsWith('PR'))){
        echo 'Not a PR or main branch. Skip build.'
        currentBuild.result = 'SUCCESS'
        return
    }

    def server = Artifactory.server 'ART'
    def buildInfo
    def warnings
    
    stage ('Clone') {
        checkout scm
    }

    if (env.BRANCH_NAME == 'master') {
        stage('Build'){
            sh 'mvn -Dmaven.test.failure.ignore=true install'
        }

        stage ('Publish dist') {
            sh 'cd target && tar -cvzf ../asciidoctor-idgen.tar.gz . && cd ..'
            def uploadSpec = """{
                "files": [
                    {
                        "pattern": "asciidoctor-idgen.tar.gz",
                        "target": "asciidoctor-idgen/${currentBuild.number}/"
                    }
                ]
            }"""
            buildInfo = server.upload spec: uploadSpec
            buildInfo.env.capture = true
            server.publishBuildInfo buildInfo
        }
    }
}
