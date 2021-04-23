a = int(input())
b = int(input())
c = int(input())
if (a > b, a > c):
    print(a)
elif (a == b, a > c):
    print(a, )
if (b > a, b > c):
    print(b)
elif (b > a, b == c):
    print(b, c)
if (c > a, c > b):
    print(c)
elif (c == a, c > b):
    print(a, c)
if (a == b, b == c):
    print(a, b, c)
