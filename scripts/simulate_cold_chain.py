#!/usr/bin/env python3
"""Send deterministic cold-chain readings to a Phase 4 shipment."""
import argparse, json, time, urllib.request, uuid

def send(url, token, shipment, temperature, index):
    body=json.dumps({"readingId":f"sim-{uuid.uuid4()}","deviceId":"resume-demo-sensor","temperatureC":temperature,
        "humidityPercent":65,"observedAt":time.strftime("%Y-%m-%dT%H:%M:%SZ",time.gmtime())}).encode()
    request=urllib.request.Request(f"{url}/api/v2/shipments/{shipment}/sensor-readings",data=body,method="POST",
        headers={"Authorization":f"Bearer {token}","Content-Type":"application/json"})
    with urllib.request.urlopen(request) as response: print(index,response.status,response.read().decode())

if __name__=="__main__":
    parser=argparse.ArgumentParser(); parser.add_argument("--url",default="http://localhost:8080"); parser.add_argument("--token",required=True); parser.add_argument("--shipment",required=True)
    parser.add_argument("--temperatures",default="4,5,11,6",help="Comma-separated Celsius readings; defaults include one excursion")
    args=parser.parse_args()
    for i,value in enumerate(args.temperatures.split(","),1): send(args.url,args.token,args.shipment,float(value),i)
