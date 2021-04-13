x = input()
y = input()
z = input()

flag = z % (1 + 1) == 0 and (1 < x and x < 123) or (1 > y and y > x and x > y and y < 123)


def identity(var):
    return var


if x ^ y == 1 or (
        x % 2 == 0 and (3 > x and x <= 3 and 3 <= y and y > z and z >= 5) or (
        identity(-1) + hash('hello') < 10 + 120 and 10 + 120 < hash('world') - 1)):
    print(x, y, z)
