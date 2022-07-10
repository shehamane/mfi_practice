import argparse
import collections
from json import load
from os import system, remove


def multiset_difference(a, b):
    a = collections.Counter(a)
    b = collections.Counter(b)

    difference = a - b

    as_list = []
    for item, count in difference.items():
        as_list.extend([item] * count)
    return as_list


def find_diff(config_path='./config.json'):
    with open(config_path, 'r') as config:
        data = load(config)
    expected = open(data['expected_path'], 'r')

    if data['header']:
        system(
            f"psql postgresql://{data['table']['user']}:{data['table']['password']}@"
            f"{data['table']['host']}:5432/{data['table']['db']} -c \""
            f"\copy {data['table']['name']} ({data['header']}) to './tmp.csv' (DELIMITER '{data['input']['delimiter']}');\""
            f" 1> log/checker.log 2> log/checker.log")
    else:
        system(
            f"psql postgresql://{data['table']['user']}:{data['table']['password']}@"
            f"{data['table']['host']}:5432/{data['table']['db']} -c \""
            f"\copy {data['table']['name']} to './tmp.csv' (DELIMITER '{data['input']['delimiter']}');\""
            f" 1> log/checker.log 2> log/checker.log")

    with open('./tmp.csv', 'r') as tmp:
        expected_lines = expected.readlines()
        actual_lines = tmp.readlines()
        diff1 = multiset_difference(expected_lines, actual_lines)
        diff2 = multiset_difference(actual_lines, expected_lines)

    flag = False
    with open("log.txt", 'w') as log:
        if diff2:
            flag = True
            log.write("\n=====================\nUnexpected data:\n")
            for line in diff2:
                log.write(line)
        if diff1:
            flag = True
            log.write("\n=====================\nMissing expected data:\n")
            for line in diff1:
                log.write(line)

    expected.close()
    remove('./tmp.csv')
    return flag
