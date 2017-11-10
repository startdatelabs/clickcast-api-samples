@Grapes([
  @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' )
])

import groovyx.net.http.*
import org.apache.commons.cli.Option

class CCApi {
  def body, host, isLive, prefix, token, zeros

  // live API
  static final API_HOST = 'https://api.clickcast.cloud'
  static final PATH_PREFIX = '/clickcast'

  // dev API
  static final DEV_API_HOST = 'https://api-dev.clickcast.cloud'
  static final DEV_PATH_PREFIX = '/clickcast-dev'

  // colors
  final static def NC = '\033[0m'
  final static def RED = '\033[0;31m'
  final static def YELLOW = '\033[0;33m'
  final static def MAGENTA = '\033[0;35m'

  // usage string
  final static USAGE = 'ccapi.sh -t token [-l] cmd type [args] [< json]'

  // default fields
  final def BUDGET_FIELDS = ['budget_id',
                             'enabled',
                             'campaign_name',
                             'campaign_id',
                             'publisher_name',
                             'publisher_id',
                             'cpc',
                             'cpa']
  final def CAMPAIGN_FIELDS = ['campaign_name',
                               'campaign_id',
                               'employer_name',
                               'employer_id',
                               'status',
                               'priority']
  final def EMPLOYER_FIELDS = ['employer_name',
                               'employer_id',
                               'status',
                               'api_managed']
  final def JOB_FIELDS = ['job_id',
                          'job_title',
                          'campaign_name',
                          'campaign_id',
                          'employer_name',
                          'employer_id',
                          'reference',
                          'company',
                          'date_posted']
  final def PUBLISHER_FIELDS = ['publisher_name',
                                'publisher_id',
                                'enabled',
                                'group']
  final def STATS_FIELDS = ['applies',
                            'clicks',
                            'spend',
                            'jobs']

  // ctor
  CCApi(final def token, final def live, final def body, final def zeros) {
    this.token = token
    if (live) {
      this.isLive = true
      this.host = API_HOST
      this.prefix = PATH_PREFIX
    }
    else {
      this.isLive = false
      this.host = DEV_API_HOST
      this.prefix = DEV_PATH_PREFIX
    }
    this.body = body
    this.zeros = zeros
  }

  // entry point
  static void main(final String[] args) {
    // process command line
    def cli = new CliBuilder(usage: USAGE)
    cli.l(args: 0, required: false, 'Live?')
    cli.t(args: 1, required: true, 'Token')
    cli.z(args: 0, required: false, 'Zero for null')
    def opt = cli.parse(args)
    if (!opt)
      return

    // our token
    def token = opt.t

    // use live API?
    def live = opt.l

    // return zero instead of null?
    def zeros = opt.z

    // read body from stdin (if anything there)
    def body
    System.in.withReader { reader ->
      if (reader.ready()) {
        body = new StringBuilder()
        reader.eachLine { body << it }
      }
    }

    def targs = []
    opt.arguments().each { targs << it }
    def tool = new CCApi(token, live, body, zeros)
    tool.run(targs)
    System.exit(0)
  }

  // create
  def create(final def type, final def params, final def body) {
    if (isLive)
      error("Not allowed to create on live API!")
    if (!'campaign'.equals(type))
      error("Only support create campaign!")

  }

  // error
  def error(final def reason) {
    printRed("ERROR: ${reason}")
    System.exit(1)
  }

  // getMap - read arguments as list of k=v pairs
  def getMap = { args ->
    def params = [:]
    args.each { val ->
      def parts = val.split('=')
      if (parts.length != 2)
        parts = val.split(':')
      if (parts.length == 2) {
        def key = parts[0].trim()
        def value = parts[1].trim()
        // list of values can either use ^ or ,
        if (value.contains('^') || value.contains(',')) {
          def vals
          if (value.contains('^'))
            vals = value.split('\\^')
          else if (value.contains(','))
            vals = value.split(',')
          // turn into real List
          def vs = []
          vals.each { vs << it }
          params."${key}" = vs
        }
        else
          params."${key}" = value
      }
    }
    return params
  }

  // get a param
  def getParam(final def params, final def field, final def require = false) {
    def val
    if (params."${field}") {
      val = params."${field}"
      params.remove(field)
    }
    else if (require)
      error("Require param ${field}")
    return val
  }

  def printRed(final def line) {
    println("${RED}${line}${NC}")
  }

  // printTable - do our best to print a table out to console
  void printTable(final def data) {
    // flatten maps and lists
    def data2 = []
    data.each { row ->
      def line = []
      row.each { obj ->
        def value = obj ?: '(null)'
        if (value instanceof List) {
          line << value.join(', ')
        }
        else if (value instanceof Map) {
          line << value.inject([]) { result, item ->
            result << "${item.key}=${item.value}"
          }.join(', ')
        }
        else {
          if (!value)
            value = ' '
          line << value.toString()
        }
      }
      data2 << line
    }

    def widths = [].withDefault { 0 }
    data2.each { row ->
      row.eachWithIndex { value, index ->
        def size = value.size()
        if (size > widths[index])
          widths[index] = size
      }
    }

    println()
    def maxLen = 0
    data2.eachWithIndex { row, lineNum ->
      def line = []
      row.eachWithIndex { value, index ->
        def pad = widths[index]
        line << value.take(pad).padRight(pad+2)
      }
      def theLine = line.join()
      maxLen = (theLine.size() > maxLen) ? theLine.size() : maxLen
      def lineColor = (lineNum == 0) ? RED : NC
      println("${lineColor}${theLine}${NC}")
      if (lineNum == 0)
        println("${lineColor}${'-'.multiply(maxLen)}${NC}")
    }
  }

  // read
  def read(final def type, final def params) {
    def endpoint, fields
    def rparams = [:] << params

    switch (type) {
      case 'budgets':
        def campaignId = getParam(rparams, 'campaign_id', true)
        endpoint = "/api/campaign/${campaignId}/budgets"
        fields = BUDGET_FIELDS
        break
      case 'campaigns':
        def employerId = getParam(rparams, 'employer_id')
        if (employerId)
          endpoint = "/api/employer/${employerId}/campaigns"
        else
          endpoint = '/api/campaigns'
        fields = CAMPAIGN_FIELDS
        break
      case 'employers':
        endpoint = '/api/employers'
        fields = EMPLOYER_FIELDS
        break
      case 'jobs':
        def campaignId = getParam(rparams, 'campaign_id', true)
        endpoint = "/api/campaign/${campaignId}/jobs"
        fields = JOB_FIELDS
        break
      case 'publishers':
        endpoint = '/api/publishers'
        fields = PUBLISHER_FIELDS
        break
      case 'stats':
      case 'stats-employer':
        (endpoint, fields, rparams) = setupEmployerStats(rparams)
        break
      case 'stats-publisher':
        (endpoint, fields, rparams) = setupPublisherStats(rparams)
        break
      default:
        error("read: unknown type: ${type}")
    }

    readEntities(endpoint, fields, rparams)
  }

  // readEntities
  def readEntities(final def endpoint, final def defaultFields, final def params) {
    def results = request(Method.GET, endpoint, params)
    if (results) {
      printRed(">>>> Results: count: ${results.count}, pages: ${results.num_pages}, page: ${results.page}")
      def entities = results.results
      // can override default set of fields using fields=a,b,c syntax
      def fields = params.fields ?: defaultFields
      // output results
      def data = []
      data << fields
      entities.each { entity ->
        def line = []
        fields.each { field ->
          if (!entity."${field}")
            line << (zeros ? '0' : '-')
          else
            line << entity."${field}"
        }
        data << line
      }
      printTable(data)
    }
  }

  // request
  def request(final def method,
              final def path,
              final def query = null,
              final def data = null,
              final def quiet = null) {
    def json
    // api expects list to be , seperated
    def qmap = [:]
    query.each { k, v ->
      if (v instanceof List)
        qmap[k] = v.join(',')
      else
        qmap[k] = v
    }
    if (!quiet) {
      // string representation of request
      def params = qmap.collect { k, v -> "${k}=${v}" }.join('&')
      def req = "${host}${prefix}${path}" + (params ? '?' + params : '')
      printRed(">>>> ${method}: ${req}")
    }
    new HTTPBuilder(host).request(method, ContentType.JSON) {
      uri.path = prefix + path
      headers.'X-Partner-Token' = token
      if (query)
        uri.query = qmap
      if (data)
        body = JsonOutput.toJson(data)
      response.success = { resp, obj ->
        json = obj
      }
      response.failure = { resp ->
        error("response: ${resp.status}, ${resp.statusLine}")
      }
    }
    return json
  }

  // run a command
  def run(final def args) {
    // must have cmd and type
    if (args.size() < 2) {
      printRed('Must have cmd and type!')
      printRed(USAGE)
      System.exit(1)
    }

    // get cmd and type
    def cmd = args[0].toLowerCase()
    def type = args[1].toLowerCase()
    def targs = args.drop(2)

    // get rest of parameters (x=y) as map
    def params = getMap(targs)

    switch (cmd) {
      case 'create':
        create(type, params, body)
        break
      case 'read':
        read(type, params)
        break
      case 'update':
        update(type, params, body)
        break
      default:
        error("Unknown cmd: ${cmd}")
    }
  }

  // setupEmployerStats
  def setupEmployerStats(final def params) {
    def endpoint
    def fields = []

    def employerId = getParam(params, 'employer_id')

    fields.addAll(['start_date', 'end_date'])
    if (employerId) {
      endpoint = "/api/employer/${employerId}/stats"
      fields.addAll(['employer_name', 'employer_id'])
      if (params.report_by.equals('campaign'))
        fields.addAll(['campaign_name', 'campaign_id'])
      if (params.publisher_id)
        fields.addAll(['publisher_name', 'publisher_id'])
    }
    else {
      endpoint = "/api/employers/stats"
      fields.addAll(['employer_name', 'employer_id'])
    }

    fields.addAll(STATS_FIELDS)

    return [endpoint, fields, params]
  }

  // setupPublisherStats
  def setupPublisherStats(final def params) {
    def endpoint
    def fields = []

    def publisherId = getParam(params, 'publisher_id')

    fields.addAll(['start_date', 'end_date'])
    if (publisherId) {
      endpoint = "/api/publisher/${publisherId}/stats"
      fields.addAll(['publisher_name', 'publisher_id'])
      if (params.report_by.equals('employer'))
        fields.addAll(['employer_name', 'employer_id'])
      if (params.campaign_id)
        fields.addAll(['campaign_name', 'campaign_id'])
    }
    else {
      endpoint = "/api/publishers/stats"
      fields.addAll(['publisher_name', 'publisher_id'])
    }

    fields.addAll(STATS_FIELDS)

    return [endpoint, fields, params]
  }

  // update
  def update(final def type, final def params, final def body) {
    printRed("Not implemented yet!")
  }
}
