class c1:
    def c1f1(self):
        c1f1v1, c1f1v2 = 1, 2


class c2(c1):
    def c1f1(self):
        c1f1v1, c1f1v2 = 3, 4


(c1() if input() else c2()).c1f1()