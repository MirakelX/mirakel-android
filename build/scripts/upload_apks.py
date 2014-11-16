#!/usr/bin/env python2
#
# Copyright 2014 Marta Rodriguez.
#
# Licensed under the Apache License, Version 2.0 (the 'License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Uploads an apk to the alpha track."""

import sys
import argparse
import httplib2
import json

from apiclient.discovery import build
from oauth2client import client

def upload(package, service, apk, track):

  # Load the service key and email from the Google Developer Service Account json file
  service_settings = json.load(service)

  # Create an httplib2.Http object to handle our HTTP requests and authorize it
  # with the Credentials. Note that the first parameter, service_account_name,
  # is the Email address created for the Service account. It must be the email
  # address associated with the key that was created.
  credentials = client.SignedJwtAssertionCredentials(
      service_settings['client_email'],
      service_settings['private_key'],
      scope='https://www.googleapis.com/auth/androidpublisher')
  http = httplib2.Http()
  http = credentials.authorize(http)

  service = build('androidpublisher', 'v2', http=http)

  try:
    edit_request = service.edits().insert(body={}, packageName=package)
    result = edit_request.execute()
    edit_id = result['id']

    apk_response = service.edits().apks().upload(
        editId=edit_id,
        packageName=package,
        media_body=apk.name).execute()

    print 'Version code %d has been uploaded' % apk_response['versionCode']

    track_response = service.edits().tracks().update(
        editId=edit_id,
        track=track,
        packageName=package,
        body={u'versionCodes': [apk_response['versionCode']]}).execute()

    print 'Track %s is set for version code(s) %s' % (
        track_response['track'], str(track_response['versionCodes']))

    commit_request = service.edits().commit(
        editId=edit_id, packageName=package).execute()

    print 'Edit "%s" has been committed' % (commit_request['id'])

  except client.AccessTokenRefreshError, e:
    print ('The credentials have been revoked or expired, please re-run the '
           'application to re-authorize')
    raise e

def main():
  parser = argparse.ArgumentParser()
  parser.add_argument('-p', '--package', required=True, help='The package name. Example: com.android.sample')
  parser.add_argument('-s', '--service', type=argparse.FileType('r'), required=True, help='The service account json file.')
  parser.add_argument('-a', '--apk', type=argparse.FileType('r'), required=True, help='The path to the APK file to upload.')
  parser.add_argument('-t', '--track', default='alpha')
  args = parser.parse_args()

  upload(args.package, args.service, args.apk, args.track)

if __name__ == "__main__":
  main()
