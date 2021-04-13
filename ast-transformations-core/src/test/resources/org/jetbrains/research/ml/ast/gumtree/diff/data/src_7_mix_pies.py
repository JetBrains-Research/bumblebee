k = 0
r = 0
a = int(input())
b = int(input())
n = int(input())
k = b * n
r = a * n
if(k > 100):
    r = r + k//100
    k = k%100
print(r, k)