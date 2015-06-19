    @Test
    public void testNewCount$COUNT() {
        int count_before=countElems();
        $CREATE;
        int count_after=countElems();
		assertEquals("Insert $TESTCLASS don't change the number of elements in database $CREATE",count_before+1,count_after);
    }
    @Test
    public void testNewInserted$COUNT() {
        List<$TESTCLASS> elems=$TESTCLASS.all();
        $TESTCLASS elem=$CREATE;
        elems.add(elem);
        List<$TESTCLASS> new_elems=$TESTCLASS.all();
        assertEquals("Something changed while adding a new element to the database $CREATE",elems,new_elems);
    }
