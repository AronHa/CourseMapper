
public class Main {
	public static void main(String[] args){
		Display display = new Display(); //Make a display
		display.run(); //Run the display
		//Mapper m = new Mapper();
		/**Course[] c = {
				new Course(3,"ABP201","Ethics in the Way of Jesus",2013,true,1,"Core",new String[]{}),
				new Course(3,"THEO201","Introduction to Theology",2013,true,1,"Core",new String[]{}),
				new Course(2,"CORE201","Life Wellness",2013,true,1,"Core",new String[]{"sophomore"}),
				new Course(1,"CORE101","Transitions",2013,true,1,"Core",new String[]{"freshman"}),
				new Course(1,"LARTS391","Peer Tutoring Practicum II",2013,true,1,"Core",new String[]{"junior"}),
				new Course(1,"MUES332","Wind Ensemble",2013,true,1,"Core",new String[]{}),
				new Course(4,"MATH170","Discrete Mathematics",2013,true,2,"Math",new String[]{}),
				new Course(3,"MATH240","Statistics for the Natural Sciences",2014,false,2,"Math",new String[]{}),
				new Course(3,"MATH350","Linear Algebra",2013,true,4,"Math",new String[]{"MATH170"}),
				new Course(1,"MATH440","Mathematics Content Portfolio",2014,false,2,"Math",new String[]{"senior"}),
				new Course(3,"MATH450","Introduction to Analysis",2015,false,4,"Math",new String[]{"MATH170","MATH350 or MATH360"}),
				new Course(3,"MATH460","Abstract Algebra",2014,false,4,"Math",new String[]{"MATH350"}),
				new Course(3,"MATH310","Differential Equations",2015,false,4,"Math",new String[]{}),
				new Course(3,"MATH333","Topics in Math",2013,true,4,"Math",new String[]{}),
				new Course(3,"MATH360","Geometry",2014,true,4,"Math",new String[]{"MATH170"}),
				new Course(3,"MATH420","History of Math",2014,false,4,"Math",new String[]{"MATH170","MATH350 or MATH360"}),
				new Course(3,"MATH470","Mathematical Probability",2014,true,4,"Math",new String[]{"MATH170","MATH240"}),
				new Course(3,"CS220","Intermediate Programming: Java",2014,false,2,"Computer Science",new String[]{}),
				new Course(3,"CS230","Networking and Data Communications",2013,true,2,"Computer Science",new String[]{}),
				new Course(3,"CS250","Architecture and Operating Systems",2013,true,2,"Computer Science",new String[]{}),
				new Course(3,"CS270","Databases and Information Management",2014,false,2,"Computer Science",new String[]{}),
				new Course(3,"CS320","Data Structures",2014,true,4,"Computer Science",new String[]{"CS220"}),
				new Course(3,"CS333","Topics in Computing",2014,false,2,"Computer Science",new String[]{"CS220"}),
				new Course(3,"CS340","Analysis of Algorithms",2015,false,4,"Computer Science",new String[]{"CS320","MATH170"}),
				new Course(3,"CS350","System Administration",2014,false,4,"Computer Science",new String[]{"CS250"}),
				new Course(3,"CS370","Software Engineering",2013,true,4,"Computer Science",new String[]{"CS220","CS270"}),
				new Course(3,"CS420","Programming Languages",2014,false,4,"Computer Science",new String[]{"CS320"}),
				new Course(3,"CS470","Project Management",2014,false,4,"Computer Science",new String[]{"CS370"}),
				new Course(4,"PHYS251","University Physics I",2013,true,2,"Physics",new String[]{}),
				new Course(4,"PHYS262","University Physics II",2014,false,1,"Physics",new String[]{"PHYS251"}),
				new Course(3,"ENGR270","Engineering Statics",2013,true,4,"Physics",new String[]{"PHYS251"}),
				new Course(3,"ENGR280","Engineering Dynamics",2015,false,4,"Physics",new String[]{"PHYS251"}),
				new Course(3,"ENGR160","Electronics",2015,false,4,"Physics",new String[]{}),
				new Course(3,"PHYS405","Thermodynamics",2014,true,4,"Physics",new String[]{}),
				new Course(3,"PHYS406","Quantum Mechanics",2013,true,4,"Physics",new String[]{}),
				new Course(3,"HONRS111","Ruling Ideas",2013,true,2,"Honors",new String[]{"freshman"}),
				new Course(3,"HONRS312","Colloquium",2014,false,2,"Honors",new String[]{}),
				new Course(2,"HONRS401","Worldview Seminar",2014,false,2,"Honors",new String[]{"senior"}),
				new Course(1,"HONRS451","Honors Capstone",2014,false,2,"Honors",new String[]{"senior"}),
				//new Course(3,"CS120","Introduction to Programming: Python",2013,true,2,"Computer Science",new String[]{}),
				new Course(3,"LANG140","Elementary Mandarin",2015,false,8,"Honors",new String[]{}),
		};
		Requirement[] r = {
				new Requirement("ABP201"),
				new Requirement("THEO201"),
				new Requirement("CORE201"),
				new Requirement("CORE101"),
				new Requirement("LARTS391"),
				new Requirement("MUES332"),
				new Requirement("HONRS111"),
				new Requirement("HONRS312"),
				new Requirement("HONRS401"),
				new Requirement("HONRS451"),
				new Requirement("MATH170"),
				new Requirement("MATH240"),
				new Requirement("MATH350"),
				new Requirement("MATH440"),
				new Requirement("MATH450"),
				new Requirement(new String[]{"MATH310","MATH333","MATH360","MATH420","MATH470"},4),
				new Requirement("CS220"),
				new Requirement("CS230"),
				new Requirement("CS250"),
				new Requirement("CS270"),
				new Requirement(new String[]{"CS320","CS333","CS340","CS350","CS370","CS420","CS470"},6),
				new Requirement("PHYS251"),
				new Requirement("PHYS262"),
				new Requirement(new String[]{"ENGR270","ENGR280","ENGR160","PHYS405","PHYS406"},3),
				new Requirement("LANG140")
		};*/
		/**Course[] c = {
				new Course(6,"CORE101","Name1",2013,true,8,"Core",new String[]{}),
				new Course(6,"CORE102","Name2",2013,true,8,"Core",new String[]{}),
				new Course(6,"CORE103","Name3",2013,true,8,"Core",new String[]{}),
				new Course(6,"CORE104","Name4",2013,true,8,"Core",new String[]{}),
				new Course(6,"CORE201","Name5",2014,false,8,"Core",new String[]{"CORE101"}),
				new Course(6,"CORE202","Name6",2014,false,8,"Core",new String[]{"CORE101"}),
				new Course(6,"CORE203","Name7",2014,false,8,"Core",new String[]{"CORE102","CORE103"}),
				new Course(6,"CORE204","Name8",2014,false,8,"Core",new String[]{}),
				new Course(6,"CORE301","Name9",2014,true,8,"Core",new String[]{"CORE204"}),
				new Course(6,"CORE302","Name10",2014,true,8,"Core",new String[]{"CORE204"}),
				new Course(6,"CORE303","Name11",2014,true,8,"Core",new String[]{"CORE204"}),
				new Course(6,"CORE304","Name12",2014,true,8,"Core",new String[]{"CORE203"})
		};
		Requirement[] r = {
				new Requirement("CORE101"),
				new Requirement("CORE102"),
				new Requirement("CORE103"),
				new Requirement("CORE104"),
				new Requirement("CORE201"),
				new Requirement("CORE202"),
				new Requirement("CORE203"),
				new Requirement("CORE204"),
				new Requirement("CORE301"),
				new Requirement("CORE302"),
				new Requirement("CORE303"),
				new Requirement("CORE304")
		};*/
		/**Course[] c = {
				new Course(3,"CORE101","TEST",2016,true,8,"Core",new String[]{}),
		};
		Requirement[] r = {
				new Requirement("CORE101"),
		};
		//Course[] test = m.network_flow_mapping(c,r);
		Course[][] test = m.create_mapping(c,r);
		System.out.println(test.length);
		for (Course[] t : test){
			for (Course t2 : t){
				if (t2 == null){
					break;
				}
				System.out.println(t2.get_name());
			}
			System.out.println("======");
		}*/
	}
}
