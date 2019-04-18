node {
        def server = Artifactory.server 'ART'
        def rtMaven = Artifactory.newMavenBuild()
        def buildInfo
        def descriptor
        def releaseVersion
        def isRelease = (env.BRANCH_NAME == 'master')
        def needSkip = false

        stage ('Clone') {
            checkout scm

            if (isRelease) {
                sh '''git checkout master'''
            }

            def changeLogSets = currentBuild.changeSets
            def lastChangeLog = changeLogSets[0]
            if (lastChangeLog != null) {
                def commitMessage = changeLogSets[0].last().msg
                needSkip = commitMessage.contains("[ci skip]")
            }
        }

        if (needSkip) {
            echo "[ci skip] found in last commit message -> returning with SUCCESS"
            currentBuild.result = 'SUCCESS'
            return
        }

        stage ('Artifactory configuration') {
            rtMaven.tool = 'M3'

            buildInfo = Artifactory.newBuildInfo()
            buildInfo.env.capture = true
        }

        if (isRelease) {
            stage ("Transform pom for release") {
                descriptor = Artifactory.mavenDescriptor()
                pom = readMavenPom file: 'pom.xml'
                releaseVersion = pom.version.split('-')[0]
                descriptor.version = releaseVersion
                descriptor.failOnSnapshot = true
                descriptor.transform()
            }
        }

            stage ('Exec Maven') {
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
