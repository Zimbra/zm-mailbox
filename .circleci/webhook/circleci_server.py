from flask import Flask, request
from cStringIO import StringIO
import json
import markdown2
import os
import requests
import sqlite3

DB_NAME = 'circleci.db'

NOTIFICATION_URL = ''
CONFIG = {}

branch_status = {}

def load_configuration():
    global CONFIG
    print("loading configuration file: config.json")
    if os.path.exists('config.json'):
        CONFIG = json.loads(open('config.json', 'r').read())
    else:
        with open('config.json', 'w') as f:
            f.write('''{"auth_token": "<auth token goes here>",
                        "URL": "<HipChat Room Notification URL goes here>"}''')
        print('''"config.json" was empty or didn't exist so a basic one was created.
        \nHipChat notifications WILL NOT function''')
        print("Please see the README.md file for information on how to update it.")


def create_tables():
    # SQLITE Types: TEXT, INTEGER, REAL, BLOB and NULL
    conn = sqlite3.connect(DB_NAME)
    c = conn.cursor()
    c.execute('''CREATE TABLE IF NOT EXISTS builds (branch TEXT PRIMARY KEY, build_num INTEGER, status TEXT, author TEXT, build_url TEXT, build_time INTEGER, raw_value TEXT)''')
    c.execute('''CREATE TABLE IF NOT EXISTS build_stats (branch TEXT PRIMARY KEY, success INTEGER, failure INTEGER)''')
    conn.commit()


def store_result(conn, result):
    c = conn.cursor()
    c.execute('''INSERT OR REPLACE INTO builds (branch, build_num, status, author, build_url, build_time, raw_value)
                 VALUES (?, ?, ?, ?, ?, ?, ?)''',
              (result['branch'], result['build_num'], result['status'], result['author'], result['build_url'], result['build_time'], json.dumps(result['raw'])))
    conn.commit()


def load_data():
    '''Loads the initial build information into the in-memory cache'''
    conn = sqlite3.connect(DB_NAME)
    c = conn.cursor()
    c.execute("SELECT branch,build_num, status, author, build_url, build_time, raw_value from builds")
    for branch, build_num, status, author, build_url, build_time, raw_value in c.fetchall():
        raw = json.loads(raw_value)
        branch_status[branch] = {
            'build_num': build_num,
            'status': status,
            'author_name': author,
            'build_url': build_url,
            'build_time': build_time,
            'details': raw.get('all_commit_details')[0],
            'raw': raw,
        }

    
def update_stats(conn, result):
    c = conn.cursor()
    c.execute("SELECT success,failure FROM build_stats WHERE branch=:branch", result)
    entry = c.fetchone()
    num_success = 1 if result['status'] == 'success' else 0
    num_failure = 1 if result['status'] != 'success' else 0
    if entry is not None:
        num_success += entry[0]
        num_failure += entry[1]

    c.execute('''INSERT OR REPLACE INTO build_stats (branch, success, failure) VALUES (?, ?, ?)''',
              (result['branch'], num_success, num_failure))
    conn.commit()

        
def notify_hipchat_channel(status):
    if not CONFIG.get('URL'):
        print("Cowardly refusing to submit notification to EMPTY notification URL")
        return
    print("Submitting 'status': %s" % status)
    requests.post(CONFIG.get('URL'),
                  data=status,
                  headers={'Content-Type': 'application/json',
                           'Authorization': 'Bearer %s' % CONFIG.get('auth_token')})

def process_circleci_payload(payload):
    payload = payload if type(payload) == dict else json.loads(payload)
    payload = payload.get('payload', {})
    value = {
        'branch': payload.get('branch'),
        'build_num': payload.get('build_num'),
        'build_time': payload.get('build_time_millis'),
        'status': payload.get('outcome', 'fail'),
        'details': payload.get('all_commit_details')[0],
        'author': '%s' % (payload['author_name']),
        'build_url': payload.get('build_url'),
    }
    value['raw'] = payload
    branch_status[value['branch']] = value
    return value

def build_status_message(branches=None):
    if branches is None or not len(branches):
        branch_statuses = branch_status.iteritems()
    else:
        branch_statuses = [(branch, branch_status.get(branch)) for branch in branches if branch in branch_status]

    all_success = True
    out = StringIO()
    out.write('|Branch|Status|Author|Triggering Commit|Message|Build URL|Start Time|Stop Time|\n')
    out.write('|------|------|------|-----------------|-------|---------|----------|---------|\n')
    for branch, build in branch_statuses:
        out.write("|%s|%s|%s|[%s](%s)|%s|[Build Details](%s)|%s|%s|\n" % (branch,
                                                                          build['status'],
                                                                          build['raw']['author_name'],
                                                                          build['details']['commit'][:10],
                                                                          build['details']['commit_url'],
                                                                          build['details']['subject'],
                                                                          build['build_url'],
                                                                          build['raw']['start_time'],
                                                                          build['raw']['stop_time']))
        if 'success' != build['status']:
            all_success = False
    out.write('|||||||||')

    status = out.getvalue()
    markdown = markdown2.markdown(status, extras=["tables"])
    response = {
        "color": "green" if all_success else "yellow",
        "message": markdown,
        "notify": False,
        "message_format": "html"
    }

    return json.dumps(response)

app = Flask(__name__)
    
@app.route('/post-status', methods=['POST'])
def status():
    value = process_circleci_payload(request.get_json())
    conn = sqlite3.connect(DB_NAME)
    store_result(conn, value)
    update_stats(conn, value)
    status_msg = build_status_message([value['branch']])
    notify_hipchat_channel(status_msg)
    return ''


@app.route('/build-status', methods=['POST'])
def status_command():
    payload = request.get_json()
    print(json.dumps(payload, indent=4))
    args = filter(lambda x: x,
                  map(lambda x: x.strip(),
                      payload['item']['message']['message'].split('/build-status')))
    return build_status_message(args)



    
load_configuration()
create_tables()
load_data()
