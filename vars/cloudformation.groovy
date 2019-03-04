/***********************************
cloudformation DSL

performs cloudformation operations

example usage
cloudformation
  stackName: 'dev'
  queryType: 'element' | 'output' ,  # either queryType or action should be supplied
  query: 'mysubstack.logicalname1' | 'outputKey', # depending on queryType
  action: 'create'|'update'|'delete',
  region: 'ap-southeast-2',
  templateUrl: 'https://s3.amazonaws.com/mybucket/cloudformation/app/master.json',
  parameters: [
    'ENVIRONMENT_NAME' : 'dev',
  ],
  accountId: '1234567890' #the aws account Id you want the stack operation performed in
  role: 'myrole' # the role to assume from the account the pipeline is running from
)

If you omit the templateUrl then for updates it will use the existing template

************************************/

@Grab(group='com.amazonaws', module='aws-java-sdk-cloudformation', version='1.11.359')
@Grab(group='com.amazonaws', module='aws-java-sdk-iam', version='1.11.359')
@Grab(group='com.amazonaws', module='aws-java-sdk-sts', version='1.11.359')
@Grab(group='com.amazonaws', module='aws-java-sdk-s3', version='1.11.359')
@Grab(group='com.amazonaws', module='aws-java-sdk-ssm', version='1.11.359')

@Grab(group='org.yaml', module='snakeyaml', version='1.23')

import com.amazonaws.auth.*
import com.amazonaws.regions.*
import com.amazonaws.services.cloudformation.*
import com.amazonaws.services.cloudformation.model.*
import com.amazonaws.services.s3.*
import com.amazonaws.services.s3.model.*
import com.amazonaws.services.securitytoken.*
import com.amazonaws.services.securitytoken.model.*
import com.amazonaws.services.simplesystemsmanagement.*
import com.amazonaws.services.simplesystemsmanagement.model.*
import com.amazonaws.waiters.*
import org.yaml.snakeyaml.Yaml
import groovy.json.JsonSlurperClassic
import java.util.concurrent.*
import java.io.InputStreamReader

def call(body) {
  println "check 1"
  sleep(10)
  def config = body
  println "check 2"
  sleep(10)
  def cf = setupCfClient(config.region, config.accountId, config.role)
  println "check 3"
  sleep(10)
  println "check 4"
  return true
}

@NonCPS
def setupCfClient(region, awsAccountId = null, role =  null) {
  def cb = AmazonCloudFormationClientBuilder.standard().withRegion(region)
  def creds = getCredentials(awsAccountId, region, role)
  // if(creds != null) {
  //   cb.withCredentials(new AWSStaticCredentialsProvider(creds))
  // }
  return cb.build()
}

@NonCPS
def getCredentials(awsAccountId, region, roleName) {
  if(env['AWS_SESSION_TOKEN'] != null) {
    return new BasicSessionCredentials(
      env['AWS_ACCESS_KEY_ID'],
      env['AWS_SECRET_ACCESS_KEY'],
      env['AWS_SESSION_TOKEN']
    )
  } else if(awsAccountId != null && roleName != null) {
    def stsCreds = assumeRole(awsAccountId, region, roleName)
    return new BasicSessionCredentials(
      stsCreds.getAccessKeyId(),
      stsCreds.getSecretAccessKey(),
      stsCreds.getSessionToken()
    )
  } else {
    return null
  }
}

@NonCPS
def assumeRole(awsAccountId, region, roleName) {
  def roleArn = "arn:aws:iam::" + awsAccountId + ":role/" + roleName
  def roleSessionName = "sts-session-" + awsAccountId
  println "assuming IAM role ${roleArn}"
  def sts = new AWSSecurityTokenServiceClient()
  if (!region.equals("us-east-1")) {
      sts.setEndpoint("sts." + region + ".amazonaws.com")
  }
  def assumeRoleResult = sts.assumeRole(new AssumeRoleRequest()
            .withRoleArn(roleArn).withDurationSeconds(3600)
            .withRoleSessionName(roleSessionName))
  return assumeRoleResult.getCredentials()
}
