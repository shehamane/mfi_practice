import argparse
import os
from json import load
from checker import find_diff
from shutil import copytree, rmtree


def truncate(table):
    os.system(f"psql postgresql://{table['user']}:{table['password']}@"
              f"{table['host']}:5432/{table['db']} -c "
              f"\"truncate {table['name']};\" 1> /dev/null 2> /dev/null")


def check_test(dir_path):
    os.chdir(dir_path)
    with open("config.json", 'r') as config:
        data = load(config)

    if not os.path.exists("log"):
        os.mkdir("log")
    truncate(data['table'])

    if os.path.exists(data['input']['tmp_dir_path']):
        rmtree(data['input']['tmp_dir_path'])
    copytree(data['input']['dir_path'], data['input']['tmp_dir_path'])

    os.system(data['loader_command'])

    isPassed = not find_diff('config.json')
    if isPassed:
        rmtree("log")
        os.remove("log.txt")

    if os.path.exists(data['input']['dir_path']):
        rmtree(data['input']['dir_path'])
    os.rename(data['input']['tmp_dir_path'], data['input']['dir_path'])
    os.chdir(f'../../')

    return isPassed


def check_tests(dir_path):
    test_id = 1
    tests = os.listdir(dir_path)
    for test in tests:
        if check_test(f"{dir_path}/{test}"):
            print(f"{test_id}/{len(tests)} {test} OK")
        else:
            print(f"{test_id}/{len(tests)} {test} FAIL")

        test_id += 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--dir')
    parser.add_argument('--test')
    args = parser.parse_args()
    if args.dir:
        check_tests(args.dir)
    elif args.test:
        print(f"{'OK' if check_test(args.test) else 'FAIL'}")
