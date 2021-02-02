import random

def impure_foo(s, y):
    i = random.randint(1, 100)
    if i >= 35:
        s = i
        x = 1
        y = x
        print(y)
    else:
        s = 10
        x = 1
        y = x
        print(y)
