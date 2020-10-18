def main():
    x = 4
    while x % 4 == 0:
        if x == 3:
            print(1)
            print(2)
            print(3)
            return
        pass

    print("ok")
    return 4


if __name__ == '__main__':
    main()
    print("end")

