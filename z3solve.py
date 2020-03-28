import socket
import time
from z3 import *

HOST = 'localhost'  
PORT = 25241        

def split

def solve(func):
    fh = open('solver_helper.py', 'w+')
    fh.write('from z3 import *')

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, PORT))
    s.listen()
    while True:
        conn, addr = s.accept()
        with conn:
            print('Connected by', addr)
            data = conn.recv(1024)
            data = data.decode("utf-8")
            print("received: " + data)
            conn.sendall("DONE\n".encode("utf-8"))
            conn.close()
            # TODO: write z3 stuff and send answers