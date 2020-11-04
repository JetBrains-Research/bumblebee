x = int(input())

t1 = 1 and x
t2 = 'Hello, world!' and x
t3 = [1, 2] and x
t4 = (1, 2) and x
t5 = {'foo': 'bar'} and x

f1 = 0 or x
f2 = '' or x
f3 = [] or x
f4 = () or x
f5 = {} or x

# Compound values on the left:
ct1 = 1 + 1 and x
ct2 = 'Hello, ' + 'world!' and x
ct3 = [1] + [2] and x
ct4 = (1,) + (2,) and x

cf1 = 1 - 1 or x
cf2 = '' + '' or x
cf3 = [] + [] or x
cf4 = () + () or x

# Should retain expressions on the left:
a = 1 + 1 or x
b = [1, 2, 3] or x
c = 1 - 1 and x
d = [] and x