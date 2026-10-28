#!/usr/bin/env python3
"""Mutating integration test: creates ONE test Ticket in local WorkHub DB.
Run only on a disposable/test DB after applying v2_plus_migration.sql.
"""
import base64
from concurrent.futures import ThreadPoolExecutor
import getpass
import http.cookiejar
import json
import time
import urllib.error
import urllib.request
import uuid

BASE='http://localhost:8080'
USER=input('Manager username [Tom]: ').strip() or 'Tom'
PASSWORD=getpass.getpass('WorkHub password: ')
KEY='integration-'+str(uuid.uuid4())
PAYLOAD=json.dumps({'title':'IDEMPOTENCY_TEST_'+KEY,'description':'Safe duplicate-write test',
                    'category':'TEST'},ensure_ascii=False).encode()


def post(body):
    cookies=http.cookiejar.CookieJar()
    opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies))
    basic=base64.b64encode(f'{USER}:{PASSWORD}'.encode()).decode()
    def request(path, method='GET', data=None, headers=None):
        h={'Authorization':'Basic '+basic,'Accept':'application/json'}
        h.update(headers or {})
        return opener.open(urllib.request.Request(BASE+path,data=data,method=method,headers=h),timeout=15)
    with request('/csrf') as response:
        csrf=json.loads(response.read())
    h={csrf['headerName']:csrf['token'], 'Idempotency-Key':KEY, 'Content-Type':'application/json'}
    try:
        with request('/tickets','POST',body,h) as response:
            return response.status,response.read().decode()
    except urllib.error.HTTPError as ex:
        return ex.code,ex.read().decode()[:200]


with ThreadPoolExecutor(max_workers=2) as pool:
    r1,r2=list(pool.map(post,[PAYLOAD,PAYLOAD]))
print('Same key / same body:',r1,r2)
assert r1[0]==200 and r2[0]==200 and r1[1]==r2[1], 'Concurrent idempotency failed'
changed=json.dumps({'title':'CHANGED','description':'Safe duplicate-write test',
                    'category':'TEST'}).encode()
r3=post(changed)
print('Same key / different body:',r3)
assert r3[0]==409, 'Expected HTTP 409 on key reuse with different body'
print('PASS: duplicate requests returned the same Ticket ID; changed body rejected')
