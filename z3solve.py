import socket
import time
HOST = 'localhost'  
PORT = 25241        

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.bind((HOST, PORT))
    s.listen()
    conn, addr = s.accept()
    with conn:
        print('Connected by', addr)
        while True:
            data = conn.recv(1024)
            if not data:
                break
            data = data.decode("utf-8")
            print("received: " + data)
            # TODO: write z3 stuff and send answers