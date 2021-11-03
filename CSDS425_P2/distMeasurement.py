# EECS425 Project 2
# Author: Hao Li
# Email: hxl1033@case.edu
# Late modified time: 2019-12-02--05:25
# Note: For ALL comments below, 'Host' indicates this machine, 'Server' indicates the far-away ...
# ... website server.
# Note 2: If you cannot compile this program, try to compile it in Terminal by typing
# 'sudo python3 FILE_PATH'. It was probably because your IDE do not have root privilege.
from struct import *
from socket import *
from time import *

FILE_PATH = 'targets.txt'
MAX_TTL = 255
SERVER_UDP_RECEIVING_PORT = 33434
TIMEOUT_TIME = 3


# The following method is to load the file and remove '\n' in each line.
# Remember: Put the 'target.txt' file in the same directory as this file!
def load_file(filename):
    list_of_websites = []
    with open(filename, 'r') as file:
        file_content = file.readlines()
    for line in file_content:
        list_of_websites.append(line.strip('\n'))
    return list_of_websites


# The following method is to check if the receiver of UDP and ICMP sender have the same IP address.
def check_ip_match(udp_server_ip, recv_pkt):
    temp = []
    for i in range(12, 16):
        temp.append(ord(recv_pkt[i:i + 1]))
        temp[i - 12] = str(temp[i - 12])
    # The following command is to convert a list to a string, and connect items in the list with a '.'.
    recv_pkt_server_ip = '.'.join(temp)
    return recv_pkt_server_ip == udp_server_ip


# The following method is to check if the receiver of UDP and ICMP sender have the same port number.
def check_port_number_match(udp_server_port_number, recv_pkt):
    # The reason why I picked 50:52 is that it's the field of port No. of UDP packet(it's in payload!)
    recv_pkt_server_port_number = unpack("!H", recv_pkt[50:52])[0]
    return udp_server_port_number == recv_pkt_server_port_number


# The following method is to check if the ICMP packet is 'port unreachable' error.
def check_icmp_type(recv_pkt):
    return recv_pkt[20] == 3 and recv_pkt[21] == 3


# The following method is to check how many bytes left in the original payload.
def check_payload_length(recv_pkt):
    return 0 if len(recv_pkt) <= 56 else len(recv_pkt) - 56


# As the instructions claimed, Host should send UDP packets to destination Server.
def measurement(dest_website, dest_port):
    # The following info is about Server.
    dest_ip_addr = gethostbyname(dest_website)
    dest_port = int(dest_port)

    # Setting up a socket to send UDP packets, with protocol IPv4(AF_INET) and ...
    # ... datagram sending(SOCK_DGRAM).
    udp_skt = socket(AF_INET, SOCK_DGRAM)
    udp_skt.setsockopt(IPPROTO_IP, IP_TTL, MAX_TTL)
    message = "measurement for class project. questions to professor mxr136@case.edu"
    payload = bytes(message + 'a' * (1472 - len(message)), 'ascii')

    # Send the UDP packet and mark down current time stamp.
    udp_skt.sendto(payload, (dest_ip_addr, dest_port))
    udp_now_sent = time()
    udp_skt.close()

    # Setting up a socket to listen and receive ICMP packets, with raw_socket.
    icmp_skt = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP)
    icmp_skt.bind(("", 0))
    # The timeout time has been configured via the following command.
    icmp_skt.settimeout(TIMEOUT_TIME)

    # Try to receive the expected ICMP packet, because some websites don't want to answer our UDP packets.
    try:
        received_packet = icmp_skt.recv(1600)
        icmp_now_received = time()
    except:
        print('Oh shoot! Your query to <' + str(dest_website) + '> has lost its way :-(')
        return None
    icmp_skt.close()

    rtt_time = icmp_now_received - udp_now_sent
    # The reason why I take No.36 byte is that, this byte is current TTL field.
    # To be more specific, the expected format of the received packet is shown as follows.
    # IP_header_of_the_received_ICMP_pkt(20 bytes) + ICMP_header(8 bytes) + ICMP_payload(...
    # ...IP_header_of_the_previously_sent_UDP_pkt(20 bytes) + UDP_header(8 bytes) + ...
    # ...UDP_payload("measurement for ..."))
    # So 20 + 8 + 8 = 36.
    remaining_ttl = received_packet[36]
    hop_count = MAX_TTL - remaining_ttl
    ip_checking = check_ip_match(dest_ip_addr, received_packet)
    port_number_checking = check_port_number_match(dest_port, received_packet)
    icmp_type_checking = check_icmp_type(received_packet)
    payload_length_checking = check_payload_length(received_packet)
    return [dest_website, dest_ip_addr, rtt_time, hop_count, ip_checking, port_number_checking,
            icmp_type_checking, payload_length_checking]


# Outputting collected results.
def output(info):
    if info is not None:
        report = 'Website name: <' + str(info[0]) + '>\nServer IP address: ' + str(info[1]) + \
                 '\nRTT time: ' + str(info[2]) + '\nHops passed through: ' + str(info[3]) + \
                 '\nSame IP addr?: ' + str(info[4]) + '\nSame port number?: ' + str(info[5]) + \
                 '\nCorrect ICMP error msg type?: ' + str(info[6]) + '\nRemaining payload: ' + \
                 str(info[7]) + '\n-=-=-=-=-=-=-=-=-=-=-=-'
        print(report)
    else:
        print('-=-=-=-=-=-=-=-=-=-=-=-')


# The main program.
def main():
    websites = load_file(FILE_PATH)
    print('-=-=-=-=-=-=-=-=-=-=-=-')
    for website in websites:
        print('*** No. ' + str(websites.index(website) + 1) + ' website ***')
        result = measurement(website, SERVER_UDP_RECEIVING_PORT)
        output(result)


if __name__ == '__main__':
    main()
