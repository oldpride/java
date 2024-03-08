package tian;

/**
 * DoubleLinkedList
 * 
 */

public class DoubleLinkedList {
    private class Node {
        int data;
        Node next;
        Node prev;

        Node(int data) {
            this.data = data;
            this.next = null;
            this.prev = null;
        }
    }

    private Node head;
    private Node tail;
    private int size;

    DoubleLinkedList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    public void add(int data) {
        Node newNode = new Node(data);
        if (this.head == null) {
            this.head = newNode;
            this.tail = newNode;
        } else {
            this.tail.next = newNode;
            newNode.prev = this.tail;
            this.tail = newNode;
        }
        this.size++;
    }

    public void getNext() {
        Node current = this.head;
        while (current != null) {
            System.out.println(current.data);
            current = current.next;
        }
    }

    public void getPrev() {
        Node current = this.tail;
        while (current != null) {
            System.out.println(current.data);
            current = current.prev;
        }
    }

    public int getSize() {
        return this.size;
    }

}
