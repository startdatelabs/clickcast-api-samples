import './polyfills'

import * as commander from 'commander'

import axios from 'axios'
import chalk from 'chalk'
import token from './token'

const Table = require('easy-table');

// our token
var theToken: string = token;
// map of other params from command line
var params = new Map<string, any>();
// live API
var baseURL = 'https://api.clickcast.cloud/clickcast';

// default fields for types
const BUDGET_FIELDS: string[] = [
  'budget_id',
  'enabled',
  'campaign_name',
  'campaign_id',
  'publisher_name',
  'publisher_id',
  'cpc',
  'cpa'
];
const CAMPAIGN_FIELDS: string[] = [
  'campaign_name',
  'campaign_id',
  'employer_name',
  'employer_id',
  'status',
  'priority'
];
const EMPLOYER_FIELDS: string[] = [
  'employer_name',
  'employer_id',
  'status',
  'api_managed'
];

// define cli entry points
commander
  .command('read <type> [options...]')
  .alias('r')
  .description('Read entities')
  .action((type, options) => {
    processOptions(options);
    setup();
    read(type);
  });
commander.parse(process.argv);

function error(message: string) {
  console.log(chalk.red(`ERROR: ${message}`));
  process.exit();
}

function getFields(fields: string[]) {
  if (params.has('fields')) {
    const flds = params.get('fields')
    params.delete('fields');
    return flds;
  }
  else
    return fields;
}

function getParam(key: string) {
  if (params.has(key)) {
    const val = params.get(key);
    params.delete(key);
    return val;
  }
  else
    return undefined;
}

function processOptions(options: string[]) {
  options.forEach((opt: string) => {
    var parts = opt.split("=", 2);
    if (parts.length != 2) {
      console.log(chalk.red('All options must be passed as field=value or field=v1,v2,v3 or field=v1^v2^v3. Found: %s'), opt);
      process.exit();
    }
    var key = parts[0];
    var value = parts[1];
    // look for well known params
    if ((key == "token") || (key == "t"))
      theToken = value;
    else {
      if (value.includes(","))
        params.set(key, value.split("^"));
      else if (value.includes("^"))
        params.set(key, value.split("^"));
      else
        params.set(key, value);
    }
  });
  // must have token!
  if (!theToken)
    error("Must pass token on command line or add it to token.ts.");
  // DEBUG
  console.log(chalk.yellow('token: %s'), theToken);
  params.forEach((value: any, key: string) => {
    console.log(chalk.yellow('param: key: %s, value: %s'), key, value);
  });
}

function read(type: string) {
  console.log(chalk.green('read -- type: %s'), type)
  var endpoint: string = '';
  var fields: string[] = [];
  switch (type) {
    case 'budgets': {
      const campaignId = getParam('campaign_id');
      if (campaignId)
        endpoint = `/api/campaign/${campaignId}/budgets`
      else
        error('budgets requires campaign_id parameter');
      fields = getFields(BUDGET_FIELDS);
      break;
    }
    case 'campaigns': {
      const employerId = getParam('employer_id');
      if (employerId)
        endpoint = `/api/employer/${employerId}/campaigns`;
      else
        endpoint = '/api/campaigns'
      fields = getFields(CAMPAIGN_FIELDS);
      break;
    }
    case 'employers': {
      endpoint = '/api/employers';
      fields = getFields(EMPLOYER_FIELDS);
      break;
    }
    default: {
      console.log(chalk.red('Unknown type: %s'), type);
      process.exit();
    }
  }

  readEntities(endpoint, fields);
}

function readEntities(endpoint: string, fields: string[]) {
  var table = new Table;
  var promise = request('get', endpoint);
  promise
    .then((results) => {
      console.log(chalk.red('Results: total: %s, num_pages: %s, page: %s'), results.count, results.num_pages, results.page);
      results.results.forEach((entity: any) => {
        fields.forEach((fld: string) => {
          table.cell(fld, entity[fld]);
        });
        table.newRow()
      });
      console.log(table.toString());
    })
    .catch((error: any) => {
      console.log(error);
    });
}

function request(method: string, endpoint: string): Promise<any> {
  var qparams: any = {};
  var results: any[] = [];

  // build up parameters for request
  params.forEach((value: any, key: string) => {
    if (value instanceof Array)
      qparams[key] = value.join(',');
    else
      qparams[key] = value;
  });

  // make request
  return new Promise((resolve, reject) => {
    axios({
      method: method,
      url: endpoint,
      params: qparams
    })
    .then((response: any) => {
      resolve(response.data);
    })
    .catch((error: any) => {
      console.log(error);
      reject(error);
    })
  });
}

function setup() {
  axios.defaults.baseURL = baseURL;
  axios.defaults.headers.common['X-Partner-Token'] = token
}
