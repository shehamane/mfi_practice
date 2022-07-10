import argparse
from json import load
import os


def load_csv(csv_path, config_path="./config.json"):
    with open(config_path, 'r') as config:
        data = load(config)

    with open(csv_path, 'r') as src:
        if data['header']:
            line = src.readline().strip('\n')
        line = src.readline().strip('\n')
        while line:
            os.system(f'echo "{line}" > tmp.csv')
            if data['header']:
                os.system(f"psql postgresql://{data['table']['user']}:{data['table']['password']}@"
                          f"{data['table']['host']}:5432/{data['table']['db']} -c \""
                          f"\copy {data['table']['name']} ({data['header']}) from 'tmp.csv' (DELIMITER '{data['input']['delimiter']}');\" "
                          f"1> log/loader.log 2> log/loader.log")
            else:
                os.system(f"psql postgresql://{data['table']['user']}:{data['table']['password']}@"
                          f"{data['table']['host']}:5432/{data['table']['db']} -c \""
                          f"\copy {data['table']['name']} from 'tmp.csv' (DELIMITER '{data['input']['delimiter']}');\" "
                          f"1> log/loader.log 2> log/loader.log")
            line = src.readline().strip('\n')


def load_files(dir_path, config_path):
    input_files = os.listdir(dir_path)
    for input_file in input_files:
        load_csv(f"{dir_path}/{input_file}", config_path)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--cfg')
    parser.add_argument('--dir')
    parser.add_argument('--csv')
    args = parser.parse_args()
    if args.dir:
        load_files(args.dir, args.cfg)
    elif args.csv:
        load_csv(args.csv, args.cfg)
