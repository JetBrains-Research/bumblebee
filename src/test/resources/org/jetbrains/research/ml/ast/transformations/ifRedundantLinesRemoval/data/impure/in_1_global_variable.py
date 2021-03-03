y = 5

def impure_foo(s):
    if s:
        x = 1
        y = x
        print(y)
        print('foo')
    else:
        x = 1
        y = x
        print(y)
        print('bar')
