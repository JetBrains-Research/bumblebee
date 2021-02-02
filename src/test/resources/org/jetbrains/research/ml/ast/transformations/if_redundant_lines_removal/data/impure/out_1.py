y = 5

def impure_foo(s):
    x = 1
    y = x
    print(y)
    if s:
        print('foo')
    else:
        print('bar')
