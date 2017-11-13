# clickcast-api-samples
Sample code for the Clickcast RESTful API

In order to use any sample code you'll need to get a valid authentication token from the API documentation and tutorial site. If you do not have an account for this site, please see your Account Manager and one will be created for you.

## GitHub repo
All the sample code is contained in this github repo. Clone it locally in order to get your own copy of the sample code along with helper scripts. From the command-line:
```
git clone https://github.com/startdatelabs/clickcast-api-samples.git
```

## Docker

A script is included, `run_docker.sh`, that will download and run a simple docker container that has the tools necessary to run all sample code. You do not need to use docker but it is provided as a convenience in case you do not have all the necessary tools installed locally.

```
cd clickcast-api-samples
./run_docker.sh
```

This will leave you at a bash prompt inside the container.

## Groovy
In the **groovy** directory you'll find the CCApi.groovy command-line app. This app demonstrates how to retrieve data from the API.

#### Introduction
The CCApi app accepts a command-line with the following format:
```
groovy CCApi.groovy -t TOKEN command type params
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
