import sys

# Bytes: 13 00 00 00 00 05 18
data = b'\x13\x00\x00\x00\x00\x05\x18'

with open('cmd.bin', 'wb') as f:
    f.write(data)

