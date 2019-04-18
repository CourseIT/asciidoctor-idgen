node {

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
          rtMaven.run pom: 'pom.xml', goals: 'clean install', buildInfo: buildInfo
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
