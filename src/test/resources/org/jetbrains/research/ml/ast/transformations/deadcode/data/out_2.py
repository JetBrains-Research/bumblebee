def main(x):
    while x % 4 == 0:
        if x == 3:
            print(1)
            print(2)
            print(3)
            continue
        pass

    for _ in range(10):
        if _ == 3:
            break

    print("ok")
    return 4
