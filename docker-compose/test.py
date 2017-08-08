#!/usr/bin/python

import requests


def check():
    data = {}

    consumers = requests.get('http://localhost:9000/consumers').json()

    for consumer_group in consumers:

        consumer_infos = requests.get(
            'http://localhost:9000/consumers/{consumer_group}'.format(
                consumer_group=consumer_group)).json()

        for partition in consumer_infos['partition_assignment']:
            data[
                '{consumer_group}-{topic}-{partition}-lag'.format(
                    consumer_group=consumer_group,
                    topic=partition['topic'],
                    partition=partition['partition'])] = partition['lag']
            data[
                '{consumer_group}-{topic}-{partition}-log_end_offset'.format(
                    consumer_group=consumer_group,
                    topic=partition['topic'],
                    partition=partition['partition'])] = partition['log_end_offset']
            data[
                '{consumer_group}-{topic}-{partition}-offset'.format(
                    consumer_group=consumer_group,
                    topic=partition['topic'],
                    partition=partition['partition'])] = partition['offset']

        print(data)

    return data


if __name__ == "__main__":
    check()
