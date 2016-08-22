"""
logs.py

A command line script used to pull down & view ACM checkout log files from the acm-logging s3 bucket.
Can be used to view logs from a specific date, month, year or can be used to view all logs from the past x days
or just the logs from y number of days ago (x & y must be integer values < 100).
By default, pulls logs from the past week and prints them out in terminal.

Usage:

In the command line type one of the following:
    python logs.py -d YYYY-M-D       (e.g. logs.py -d 2016-6-13)
    python logs.py -m YYYY-M         (e.g. logs.py -d 2016-6)
    python logs.py -y YYYY           (e.g. logs.py -d 2016)
    python logs.py x                 (e.g. logs.py 3 -- prints logs from past three days)
    python logs.py -y                (e.g. logs.py -4 -- prints logs from four days ago)

Default:
    python logs.py                   (default mode -- prints out logs from past 7 days)
"""

from __future__ import print_function
from __future__ import division
import datetime
import boto3
import argparse
import sys


# specify s3 client where logs are stored
client = boto3.client('s3', region_name='us-west-2')
bucket = 'acm-logging'


# prints out all logs from past num_days
def printer(num_days):
    for day in range(num_days):
        date = datetime.date.fromordinal(datetime.date.today().toordinal()-day)
        if day == 0 or day == 1:    # unconsolidated log files have a diff s3 dir
            base_dir = get_dir(date)
        else:
            base_dir = ['logs/' + str(date.year) + '/' + str(date.month) + '/' + 'log_' + str(date)]
        dir_printer(base_dir)


# prints all log files in specified directory
def dir_printer(base_dir):
    log_list = get_logs(base_dir)

    # early return if log_list empty
    if not log_list:
        return

    log_keys = [log.get('Key') for log in log_list]
    for key in log_keys:
        log = client.get_object(
            Bucket=bucket,
            Key=key
        )
        print(log.get('Body').read())


# returns list of logs found in specified s3 directory
def get_logs(base_dir):
    for dir in base_dir:
        resp = client.list_objects(
            Bucket=bucket,
            Prefix=dir
        )
        if resp.get('Contents'):
            return resp.get('Contents')
        else:
            continue


# get s3 directories for consolidated & unconsolidated logs
def get_dir(date):
    consol_dir = 'logs/' + str(date.year) + '/' + str(date.month) + '/' + 'log_' + str(date)
    unconsol_dir = 'logs/' + str(date.year) + '/' + str(date.month) + '/' + str(date)
    return [consol_dir, unconsol_dir]


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('days', metavar='N', nargs='?', type=int, help='Enter positive integer to retrieve logs from the past '
                                                            '<num> of days or negative integer for logs from '
                                                            '<num> of days ago. NOTE: -100 < <num> < 100')
    parser.add_argument('-d', '--date', help='Enter date in format YYYY-M-D', default=None)
    parser.add_argument('-m', '--month', help='Enter month in format YYYY-M', default=None)
    parser.add_argument('-y', '--year', help='Enter year in format YYYY', default=None)
    args = parser.parse_args()

    # early return if too many options selected
    if len(sys.argv) > 3:
        print('Too many arguments passed')
        sys.exit(1)

    # default behavior: print out past week's logs if no arguments are passed in by user
    if len(sys.argv) < 2:
        printer(7)
        sys.exit(0)

    if args.days is not None:
        if 0 < args.days < 100:
            printer(args.days)
            sys.exit(0)
        elif -100 < args.days < 0:
            args.date = datetime.date.fromordinal(datetime.date.today().toordinal()+args.days)
            base_dir = get_dir(args.date)
        else:
            print('Number out of bounds')
            sys.exit(1)

    elif args.date:
        args.date = datetime.datetime.strptime(args.date, "%Y-%m-%d").date()    # convert string to date object
        base_dir = get_dir(args.date)

    elif args.month:
        month = datetime.datetime.strptime(args.month, "%Y-%m").date()
        base_dir = ['logs/' + str(month.year) + '/' + str(month.month)]

    elif args.year:
        year = datetime.datetime.strptime(args.year, "%Y").date()
        base_dir = ['logs/' + str(year.year)]

    dir_printer(base_dir)
    sys.exit(0)
