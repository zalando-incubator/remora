#!/usr/bin/python

import sys, requests


def check():
  data={}

  consumers = requests.get('http://localhost:9000/consumers').json()

  for consumer in consumers:

      consumerInfos=requests.get('http://localhost:9000/consumers/'+consumer['groupId']).json()

      for consumerInfo in consumerInfos:
        data['{consumer_group}-{topic}-{partition}-lag'.format(consumer_group=consumer['groupId'],topic=consumerInfo['topic'],partition=consumerInfo['partition'])]=consumerInfo['lag']
        data['{consumer_group}-{topic}-{partition}-log_end_offset'.format(consumer_group=consumer['groupId'],topic=consumerInfo['topic'],partition=consumerInfo['partition'])]=consumerInfo['log_end_offset']
        data['{consumer_group}-{topic}-{partition}-offset'.format(consumer_group=consumer['groupId'],topic=consumerInfo['topic'],partition=consumerInfo['partition'])]=consumerInfo['offset']

      print(data)
 
  return data


if __name__ == "__main__":
   check()
