x = input()
y = input()
z = input()

flag = z % (1 + 1) == 0 and 1 < x < 123 or 1 > y > x > y < 123


def identity(var):
    return var


if x ^ y == 1 or (x % 2 == 0 and 3 > x <= 3 <= y > z >= 5 or identity(-1) + hash('hello') < 10 + 120 < hash('world') - 1):
    print(x, y, z)
