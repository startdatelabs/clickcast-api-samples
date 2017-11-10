# clickcast-api-samples
Sample code for the Clickcast RESTful API

In order to use any sample code you'll need to get a valid authentication token from the API documentation and tutorial site. If you do not have an account for this site, please see your Account Manager and one will be created for you.


### Groovy
In the **groovy** directory you'll find the CCApi.groovy command-line app plus a shell script, ccapi.sh, to run it. This app demonstrates how to retrieve data from the API.


### Introduction
The CCApi app accepts a command-line with the following format:
```
./ccapi.sh -t TOKEN command type params
```

Here are the valid combinations of command and type:
| Command | Type | Params | Description |
| --- | --- | --- | --- |
| create | campaign | | Create one campaign |
| read | budgets | campaign_id=X | Read budgets for one campaign |
| | campaigns | | Read campaigns |
| | campaigns | employer_id=X | Read campaigns for one employer |
| | employers | | Read employers |
| | jobs | campaign_id=X | Read jobs for one campaign |
| | publishers | | Read publishers |
| | stats | | Read stats for all employers |
| | stats-employer | | Read stats for all employers |
| | stats-employer | employer_id=X | Read stats for one employer |
| | stats-publisher | | Read stats for all publishers |
| | stats-publisher | publisher_id=X | Read stats for one publisher |
| update | budgets | campaign_id=X | Update budgets for one campaign |
|  | campaign | campaign_id=X | Update one campaign |


#### Examples

```
./ccpai.sh -t TOKEN read employers
```
This will list all of your Clickcast employers
