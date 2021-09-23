import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.label.LabelManager

String prTitle = ruleContext.renderSmartValues('{{webhookData.pull_request.title}}')
String prUser = ruleContext.renderSmartValues('{{webhookData.pull_request.user.login}}')
String prRepository = ruleContext.renderSmartValues('{{webhookData.pull_request.repository.name}}')
/*
addMessage("Title: " + prTitle)
addMessage("User: " + prUser)
addMessage("Repository: " + prRepository)
*/
def loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser

def jiraKeyRegexp = ~/((?<!([A-Z]{1,10})-?)[A-Z]+-\d+)/
// clean up the title removing ":"
prTitle = prTitle.replaceAll(":", " ")
prTitle = prTitle.replaceAll("_", " ")
// split off the jira key from the title
def jiraKeyStrings = prTitle.split(" ")
def jiraKeyString = jiraKeyStrings[0]
if (jiraKeyString == null) {
    addMessage(prTitle + " - No jira key found in title" + " user: " + prUser)
    return
}
// check for a valid Jira Key
if (!jiraKeyString.matches(jiraKeyRegexp)) {
    addMessage(prTitle + " - No valid jira key found in title"  + " user: " + prUser)
    return
}
// see if the jira key is found
IssueManager issueManager = ComponentAccessor.getIssueManager()
MutableIssue prIssue = issueManager.getIssueByKeyIgnoreCase(jiraKeyString)
if (prIssue == null){
    addMessage(prTitle + " - Not a valid jira issue")
    return
}
// check if repository is defined in the gitRepository custom field
def customFieldName = "customfield_16600"  // GitRepository
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def customField = customFieldManager.getCustomFieldObject(customFieldName)
if (customField == null) {
    addMessage("Cannot find custom field")
    return
}
// get the custom label field value
def labelManager = ComponentAccessor.getComponent(LabelManager)
def existingLabels = labelManager.getLabels(issue.id)*.label

if (existingLabels == null)
    addMessage("No values set for GitRepository")
else
    addMessage("Values for GitRepository are: " + existingLabels)
def List<String> newLabels = [prRepository]
def labelsToSet = (existingLabels + newLabels).toSet()

labelManager.setLabels(loggedInUser, prIssue.id, labelsToSet, false, false)
addMessage("Updated Jira Key: " + jiraKeyString)