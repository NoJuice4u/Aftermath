import httplib
import random
import json
import time
import argparse

def main():
	parser = argparse.ArgumentParser()
	parser.add_argument('--count', type=int, default=0)
	parser.add_argument('--delay', type=float, default=1.0)
	args = parser.parse_args()
	h1 = httplib.HTTPConnection('127.0.0.1', 8080, timeout=10)

	value = 4472707495
	request = h1.request("GET", "/aftermath/map/node/" + str(value) + "/json?depth=24&zoom=16")
	response = h1.getresponse()

	#set weights
	weightedList = {}

	smallList = json.loads(response.read())
	for item in smallList['mapEdges']:
		rndValue = random.random()
		rndValue = (1-(rndValue*rndValue)) * 255
		weightedList[item] = rndValue
		print(str(item) + ":" + str(int(rndValue)))

#Do the program loop here to work around the node selected
	i = 0
	while(i < args.count or args.count == 0):
		i += 1
		r = random.choice(weightedList.keys())

		#Submit information
		msg = ""
		v = random.choice(range(0,1))
		for edge in smallList['mapEdges'][r]['vertices'][v]['edges']:
			try:
				mlt = (random.random() * 0.25) + 0.875
				newWeight = weightedList[str(edge)] * mlt
				msg = msg + str(edge) + "=" + str(newWeight) + "&"
			except:
				pass

		msg = msg[0:len(msg)-1]
		print(str(smallList['mapEdges'][r]['vertices'][0]['id']) + ":" + msg)
		request = h1.request("POST", "/aftermath/map/weight/", msg) #URL FOR POST DATA
		response = h1.getresponse()
		response.read()
		time.sleep(args.delay)

if __name__ == "__main__": main()
