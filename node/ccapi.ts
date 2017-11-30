import './polyfills'

import * as commander from 'commander'

import { HorizontalTable } from 'cli-table2'
import axios from 'axios'
import chalk from 'chalk'

// our global token
var token: string;
// map of other params
var params = new Map<string, any>();
// live API
var baseURL = 'https://api.clickcast.cloud/clickcast';

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

function processOptions(options: string[]) {
  options.forEach((opt: string) => {
    var parts = opt.split("=", 2);
    if (parts.length != 2) {
      console.log(chalk.red('All options must be passed as field=value or field=v1,v2,v3 or field=v1^v2^v3. Found: %s'), opt);
      process.exit();
    }
    var key = parts[0];
    var value = parts[1];
    if ((key == "token") || (key == "t"))
      token = value;
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
  if (token == null) {
    console.log(chalk.red('Must specify token!'));
    process.exit();
  }
  // DEBUG
  console.log(chalk.yellow('token: %s'), token);
  params.forEach((value: any, key: string) => {
    console.log(chalk.yellow('param: key: %s, value: %s'), key, value);
  });
}

function read(type: string) {
  console.log(chalk.green('read -- type: %s'), type)
  var endpoint: string = '';
  switch (type) {
    case 'employers': {
      endpoint = '/api/employers';
      break;
    }
    default: {
      console.log(chalk.red('Unknown type: %s'), type);
      process.exit();
    }
  }

  readEntities(endpoint);
}

function readEntities(endpoint: string) {
  var promise = request('get', endpoint);
  promise.then((results) => {
    console.log(chalk.red('Results: total: %s, num_pages: %s, page: %s'), results.count, results.num_pages, results.page);
    const table = new HorizontalTable({
        head: ['Employer ID', 'Employer Name'],
        colWidths: [10, 100]
    });
    results.results.forEach((entity: any) => {
      // console.log('id: %s, name: %s', entity.employer_id, entity.employer_name);
      table.push([entity.employer_id, entity.employer_name]);
    });
    console.log(table.toString());
  });
}

function request(method: string, endpoint: string): Promise<any> {
  var results: any[] = [];
  return new Promise((resolve, reject) => {
    axios({
      method: method,
      url: endpoint
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
