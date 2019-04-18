node {
    try {
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
            rtMaven.deployer releaseRepo: 'libs-release-local', server: server
            rtMaven.resolver releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot', server: server

            if (!isRelease) {
                rtMaven.deployer.deployArtifacts = false
            }

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

        try {
            stage ('Exec Maven') {
                rtMaven.run pom: 'pom.xml', goals: 'clean install', buildInfo: buildInfo
            }
        } finally {
            junit '**/surefire-reports/**/*.xml'
           // step( [ $class: 'JacocoPublisher', execPattern: 'target/jacoco.exec' ] )
           // checkstyle pattern: 'target/checkstyle-result.xml'
        }

        if (isRelease) {
            stage ('Publish build info') {
                server.publishBuildInfo buildInfo
            }

            stage ('Update repository') {
                sh '''git add .'''

                def commitReleaseScript = "git commit -m \"updating poms for " + releaseVersion + " release [ci skip]\""
                sh commitReleaseScript
                def tagScript = "git tag " + releaseVersion
                sh tagScript

                def splittedVersion = releaseVersion.split('\\.')
                splittedVersion[2] = (splittedVersion[2].toInteger() + 1) as String
                def newSnapshotVersion = splittedVersion.join('.') + '-SNAPSHOT'
                descriptor.version = newSnapshotVersion
                descriptor.failOnSnapshot = false
                descriptor.transform()

                sh '''git add .'''
                def commitSnapshotScript = "git commit -m \"updating poms for " + newSnapshotVersion + " development [ci skip]\""
                sh commitSnapshotScript

                withCredentials([usernamePassword(credentialsId: 'curs-jenkins-bot', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                    def repoUrl = sh(returnStdout: true, script: 'git config remote.origin.url').trim().substring(8)
                    def pushUrl = 'https://' + GIT_USERNAME + ':' + GIT_PASSWORD + '@' + repoUrl

                    sh "git push $pushUrl master"
                    sh "git push $pushUrl --tags"
                }

            }
        }
    } finally {
        deleteDir() /* clean up our workspace */
    }
}
